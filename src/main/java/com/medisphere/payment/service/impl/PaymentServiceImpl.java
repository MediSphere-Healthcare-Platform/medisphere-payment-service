package com.medisphere.payment.service.impl;

import com.medisphere.payment.client.MedisphereAppointmentClient;
import com.medisphere.payment.client.MedisphereAuthClient;
import com.medisphere.payment.client.MedisphereNotificationClient;
import com.medisphere.payment.client.MedispherePatientClient;
import com.medisphere.payment.client.request.AppointmentStatusChangeClientRequest;
import com.medisphere.payment.client.request.NotificationClientRequest;
import com.medisphere.payment.client.response.AuthApiClientResponse;
import com.medisphere.payment.domain.InitiatePaymentRequest;
import com.medisphere.payment.domain.PaymentNotifyRequest;
import com.medisphere.payment.dto.response.PayHereDetailsResponse;
import com.medisphere.payment.entity.CommonUrlEntity;
import com.medisphere.payment.entity.MedispherePaymentEntity;
import com.medisphere.payment.repository.CommonUrlRepository;
import com.medisphere.payment.repository.MediSphereDoctorChargesRepository;
import com.medisphere.payment.repository.PaymentRepository;
import com.medisphere.payment.service.PaymentService;
import com.medisphere.payment.service.ResponseGenerator;
import com.medisphere.payment.util.MessageConstant;
import com.medisphere.payment.util.PayHereHasherUtil;
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
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ResponseGenerator responseGenerator;
    private final MedispherePatientClient medispherePatientClient;
    private final MedisphereAppointmentClient medisphereAppointmentClient;
    private final MedisphereAuthClient medisphereAuthClient;
    private final MedisphereNotificationClient medisphereNotificationClient;
    private final MediSphereDoctorChargesRepository doctorChargesRepository;
    private final CommonUrlRepository commonUrlRepository;
    private final PayHereHasherUtil payHereHasherUtil;

    @Value("${payhere.merchant.id}")
    private String MERCHANT_ID;

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

            String formattedAmount = new BigDecimal(request.getAmount())
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();

            String currency = request.getCurrency().toUpperCase();

            String hash = payHereHasherUtil.generateCheckoutHash(paymentReferenceId, formattedAmount, currency);

            // Fetch URLs dynamically
            String frontendBaseUrl = commonUrlRepository.findByCode("FRONTEND_BASE_URL")
                    .map(CommonUrlEntity::getUrl)
                    .orElse("http://localhost:3000");

            String apiBaseUrl = commonUrlRepository.findByCode("API_BASE_URL")
                    .map(CommonUrlEntity::getUrl)
                    .orElse("https://medisphere.requestcatcher.com");

            // Fetch email from Auth Service using msUserId
            String patientEmail = null;
            if (request.getMsUserId() != null) {
                ResponseEntity<AuthApiClientResponse<String>> authResponse = medisphereAuthClient.getEmailByMsUserId(request.getMsUserId());
                if (authResponse.getBody() != null && authResponse.getBody().getData() != null) {
                    patientEmail = authResponse.getBody().getData();
                }
            }
            if (patientEmail == null) {
                patientEmail = patientData.getEmail(); // Fallback if auth service fails or msUserId is missing
            }

            PayHereDetailsResponse response = PayHereDetailsResponse.builder()
                    .merchant_id(MERCHANT_ID.trim())
                    .order_id(paymentReferenceId.trim())
                    .items("Medical Appointment Fee")
                    .currency(currency)
                    .amount(formattedAmount)
                    .hash(hash)
                    .first_name(patientData.getFirstName())
                    .last_name(patientData.getLastName())
                    .email(patientEmail)
                    .phone(patientData.getPhoneNumber())
                    .address(patientData.getAddress() != null ? patientData.getAddress() : "N/A")
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
                    request.getOrder_id());

            // Verify Signature
            DecimalFormat df = new DecimalFormat("0.00");
            String formattedAmount = df.format(new BigDecimal(request.getPayhere_amount()));
            String localHash = payHereHasherUtil.generateNotifyHash(
                    request.getOrder_id(),
                    formattedAmount,
                    request.getPayhere_currency(),
                    request.getStatus_code()
            );

            if (!localHash.equalsIgnoreCase(request.getMd5sig())) {
                log.warn("Invalid MD5 Signature received for Order: {}. Local: {}, Received: {}",
                        request.getOrder_id(), localHash, request.getMd5sig());
                return responseGenerator.generateResponse(ResponseCode.INVALID_SIGNATURE,
                        MessageConstant.INVALID_SIGNATURE, null);
            }

            // Update Internal Record
            MedispherePaymentEntity paymentEntity = paymentRepository
                    .findByPaymentReferenceId(request.getOrder_id());
            if (paymentEntity == null) {
                log.warn("Payment not found: {}", request.getOrder_id());
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

                //Update Appointment Status to PAID
                medisphereAppointmentClient.updateAppointmentStatus(appointmentStatusChangeClientRequest);

                // Send Success Email Notification
                log.info("Sending payment success email for appointment: {}", paymentEntity.getAppointmentReferenceId());
                NotificationClientRequest notificationReq = NotificationClientRequest.builder()
                        .userId(paymentEntity.getMsUserId())
                        .userRole("PATIENT")
                        .title("Payment Confirmed - MediSphere")
                        .message("Dear Patient, your payment of " + paymentEntity.getCurrency() + " " + paymentEntity.getAmount() + 
                                 " for appointment " + paymentEntity.getAppointmentReferenceId() + " has been successfully processed. " +
                                 "Your appointment is now confirmed and marked as PAID. Thank you for choosing MediSphere.")
                        .channel("EMAIL")
                        .relatedId(paymentEntity.getAppointmentReferenceId())
                        .isBroadcast(false)
                        .build();

                try {
                    medisphereNotificationClient.createNotification(notificationReq);
                    log.info("Successfully sent payment confirmation email for reference: {}", paymentEntity.getAppointmentReferenceId());
                } catch (Exception e) {
                    log.error("Failed to send payment confirmation email: ", e);
                }

                log.info("Payment success update handled for reference: {}", request.getOrder_id());
            } else {
                paymentEntity.setStatus(Status.Failed.name());
                log.warn("Payment FAILED handled for Reference: {}. Status: {}", request.getOrder_id(),
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

    @Override
    public ResponseEntity<Object> getPaymentHistory() {
        try {
            log.debug("Fetching payment history");
            List<MedispherePaymentEntity> payments = paymentRepository.findAll();
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_SUCCESS,
                    MessageConstant.PAYMENT_OPERATION_SUCCESS, payments);
        } catch (Exception e) {
            log.error("Error fetching payment history: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

    @Override
    public ResponseEntity<Object> getDoctorCharge(String doctorId) {
        try {
            log.info("Fetching charges for doctor: {}", doctorId);
            return doctorChargesRepository.findByDoctorId(doctorId)
                    .map(charge -> responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_SUCCESS,
                            MessageConstant.PAYMENT_OPERATION_SUCCESS, charge))
                    .orElseGet(() -> {
                        log.warn("Charge not found for doctor: {}", doctorId);
                        return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                                MessageConstant.DOCTOR_CHARGES_NOT_FOUND, null);
                    });
        } catch (Exception e) {
            log.error("Error fetching doctor charges: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

    @Override
    public ResponseEntity<Object> getPaymentByOrderId(String orderId) {
        try {
            log.debug("Fetching payment details for Order: {}", orderId);
            MedispherePaymentEntity payment = paymentRepository.findByPaymentReferenceId(orderId);
            if (payment == null) {
                return responseGenerator.generateResponse(ResponseCode.PAYMENT_RECORD_NOT_FOUND,
                        MessageConstant.PAYMENT_RECORD_NOT_FOUND, null);
            }
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_SUCCESS,
                    MessageConstant.PAYMENT_OPERATION_SUCCESS, payment);
        } catch (Exception e) {
            log.error("Error fetching payment details: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

    @Override
    public ResponseEntity<Object> simulateLocalPaymentSuccess(String orderId) {
        try {
            log.info("Simulating local payment success for Order: {}", orderId);
            MedispherePaymentEntity payment = paymentRepository.findByPaymentReferenceId(orderId);
            if (payment == null) {
                return responseGenerator.generateResponse(ResponseCode.PAYMENT_RECORD_NOT_FOUND,
                        MessageConstant.PAYMENT_RECORD_NOT_FOUND, null);
            }

            // Ensure amount is formatted to 0.00 to match PayHere/Notify expectations
            DecimalFormat df = new DecimalFormat("0.00");
            String formattedAmount = df.format(new BigDecimal(payment.getAmount()));

            String statusCode = "2"; // Success status for PayHere
            String md5sig = payHereHasherUtil.generateNotifyHash(
                    orderId,
                    formattedAmount,
                    payment.getCurrency(),
                    statusCode
            );

            PaymentNotifyRequest notifyRequest = new PaymentNotifyRequest();
            notifyRequest.setMerchant_id(MERCHANT_ID);
            notifyRequest.setOrder_id(orderId);
            notifyRequest.setPayhere_amount(formattedAmount);
            notifyRequest.setPayhere_currency(payment.getCurrency());
            notifyRequest.setStatus_code(statusCode);
            notifyRequest.setMd5sig(md5sig);
            notifyRequest.setPayhere_payment_id("SIM-" + orderId);
            notifyRequest.setMethod("SIMULATED");

            return handleNotify(notifyRequest);
        } catch (Exception e) {
            log.error("Error during simulation: ", e);
            return responseGenerator.generateResponse(ResponseCode.PAYMENT_OPERATION_FAILED,
                    MessageConstant.PAYMENT_OPERATION_FAILED, null);
        }
    }

}
