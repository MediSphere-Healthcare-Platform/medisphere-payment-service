package com.medisphere.payment.service.impl;

import com.medisphere.payment.client.MedisphereAppointmentClient;
import com.medisphere.payment.client.MedispherePatientClient;
import com.medisphere.payment.client.request.AppointmentStatusChangeClientRequest;
import com.medisphere.payment.domain.InitiatePaymentRequest;
import com.medisphere.payment.domain.PaymentNotifyRequest;
import com.medisphere.payment.dto.response.PayHereDetailsResponse;
import com.medisphere.payment.entity.CommonUrlEntity;
import com.medisphere.payment.entity.MedispherePaymentEntity;
import com.medisphere.payment.repository.CommonUrlRepository;
import com.medisphere.payment.repository.PaymentRepository;
import com.medisphere.payment.service.PaymentService;
import com.medisphere.payment.service.ResponseGenerator;
import com.medisphere.payment.util.MessageConstant;
import com.medisphere.payment.util.ResponseCode;
import com.medisphere.payment.util.Utility;
import com.medisphere.payment.util.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medisphere.payment.client.response.PatientByIdClientResponse;
import com.medisphere.payment.client.response.PatientClientResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ResponseGenerator responseGenerator;
    private final MedispherePatientClient medispherePatientClient;
    private final MedisphereAppointmentClient medisphereAppointmentClient;
    private final CommonUrlRepository commonUrlRepository;

    @Value("${payhere.merchant.id}")
    private String MERCHANT_ID;

    @Value("${payhere.merchant.secret}")
    private String MERCHANT_SECRET;

    @Override
    @Transactional
    public ResponseEntity<Object> initiatePayment(InitiatePaymentRequest request) {
        try {
            log.debug(
                    "Initiate new transaction before the user is redirected to the payment gateway, for Appointment: {}",
                    request.getAppointmentReferenceId());

            // Validate Patient
            ResponseEntity<PatientByIdClientResponse> patientByIdClientResponse = medispherePatientClient
                    .getPatientById(request.getPatientId());
            log.debug("Patient service response: {}", Utility.objectToJson(patientByIdClientResponse));
            if (patientByIdClientResponse.getBody() == null || patientByIdClientResponse.getBody().getData() == null) {
                log.warn("Patient not found: {}", request.getPatientId());
                return responseGenerator.generateResponse(ResponseCode.PATIENT_NOT_FOUND,
                        MessageConstant.PATIENT_NOT_FOUND, null);
            }
            PatientClientResponse patientData = patientByIdClientResponse.getBody().getData();
            log.debug("Patient data: {}", patientData);

            String paymentReferenceId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            MedispherePaymentEntity paymentEntity = new MedispherePaymentEntity();
            paymentEntity.setPaymentReferenceId(paymentReferenceId);
            paymentEntity.setAppointmentReferenceId(request.getAppointmentReferenceId());
            paymentEntity.setPatientId(request.getPatientId());
            paymentEntity.setMsUserId(request.getMsUserId());
            paymentEntity.setAmount(request.getAmount());
            paymentEntity.setCurrency(request.getCurrency());
            paymentEntity.setStatus(Status.PENDING.name());

            paymentRepository.save(paymentEntity);

            // Generate PayHere Hash
            String formattedAmount = new BigDecimal(request.getAmount()).setScale(2, RoundingMode.HALF_UP).toString();
            String hash = generatePayHereHash(MERCHANT_ID, paymentReferenceId, formattedAmount, request.getCurrency());

            // Fetch URLs dynamically
            String frontendBaseUrl = commonUrlRepository.findByCode("FRONTEND_BASE_URL")
                    .map(CommonUrlEntity::getUrl)
                    .orElse("http://localhost:3000");

            String apiBaseUrl = commonUrlRepository.findByCode("API_BASE_URL")
                    .map(CommonUrlEntity::getUrl)
                    .orElse("https://medisphere.requestcatcher.com");

            PayHereDetailsResponse response = PayHereDetailsResponse.builder()
                    .merchant_id(MERCHANT_ID)
                    .paymentRefId(paymentReferenceId)
                    .items("Medical Appointment Fee")
                    .currency(request.getCurrency())
                    .amount(formattedAmount)
                    .hash(hash)
                    .first_name(patientData.getFirstName())
                    .last_name(patientData.getLastName())
                    .email(patientData.getEmail()) // TODO: inform dasun
                    .phone(patientData.getPhoneNumber())
                    .address(patientData.getAddress())
                    .return_url(frontendBaseUrl + "/payment-success")
                    .cancel_url(frontendBaseUrl + "/payment-cancel")
                    .notify_url(apiBaseUrl + "/payment/api/v1/payment/notify")
                    .build();

            log.info("Payment initiated successfully: {}", paymentReferenceId);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_SUCCESS,
                    MessageConstant.PAYMENT_OPERATION_SUCCESS, response);

        } catch (Exception e) {
            log.error("Error initiated payment: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Object> handleNotify(PaymentNotifyRequest request) {
        try {
            log.debug(
                    "Webhook called asynchronously by PayHere to confirm if the payment was actually successful, Called for Order: {}",
                    request.getPaymentRefId());

            // Verify Signature
            String localHash = generateNotifyHash(request);
            if (!localHash.equalsIgnoreCase(request.getMd5sig())) {
                log.warn("Invalid MD5 Signature received for Order: {}. Local: {}, Received: {}",
                        request.getPaymentRefId(), localHash, request.getMd5sig());
                return responseGenerator.generateResponse(ResponseCode.INVALID_SIGNATURE,
                        MessageConstant.INVALID_SIGNATURE, null);
            }

            // Update Internal Record
            MedispherePaymentEntity paymentEntity = paymentRepository
                    .findByPaymentReferenceId(request.getPaymentRefId());
            if (paymentEntity == null) {
                log.warn("Payment not found: {}", request.getPaymentRefId());
                return responseGenerator.generateResponse(ResponseCode.PAYMENT_RECORD_NOT_FOUND,
                        MessageConstant.PAYMENT_RECORD_NOT_FOUND, null);
            }

            if ("2".equals(request.getStatus_code())) {
                paymentEntity.setStatus(Status.Success.name());
                paymentEntity.setPayherePaymentId(request.getPayhere_payment_id());
                paymentEntity.setPayhereAmount(request.getPayhere_amount());
                paymentEntity.setPaymentMethod(request.getMethod());

                AppointmentStatusChangeClientRequest appointmentStatusChangeClientRequest = AppointmentStatusChangeClientRequest
                        .builder()
                        .appointmentReferenceId(paymentEntity.getAppointmentReferenceId())
                        .status(Status.PAID.name())
                        .build();

                // Update Appointment Status to PAID
                medisphereAppointmentClient.updateAppointmentStatus(appointmentStatusChangeClientRequest);
                log.debug("Payment SUCCESS handled for Reference: {}", request.getPaymentRefId());
            } else {
                paymentEntity.setStatus(Status.Failed.name());
                log.warn("Payment FAILED handled for Reference: {}. Status: {}", request.getPaymentRefId(),
                        request.getStatus_code());
            }

            paymentRepository.save(paymentEntity);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_SUCCESS,
                    MessageConstant.PAYMENT_OPERATION_SUCCESS, null);

        } catch (Exception e) {
            log.error("Error handling notify: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

    private String generatePayHereHash(String merchantId, String orderId, String amount, String currency) {
        String secretHash = md5Java(MERCHANT_SECRET).toUpperCase();
        String mainString = merchantId + orderId + amount + currency + secretHash;
        return md5Java(mainString).toUpperCase();
    }

    private String generateNotifyHash(PaymentNotifyRequest request) {
        String secretHash = md5Java(MERCHANT_SECRET).toUpperCase();
        String mainString = request.getMerchant_id() +
                request.getPaymentRefId() +
                request.getPayhere_amount() +
                request.getPayhere_currency() +
                request.getStatus_code() +
                secretHash;
        return md5Java(mainString).toUpperCase();
    }

    private String md5Java(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(message.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 Algorithm not found", e);
        }
    }
}
