package com.medisphere.payment.controller;

import com.medisphere.payment.domain.InitiatePaymentRequest;
import com.medisphere.payment.dto.request.InitiatePaymentRequestDTO;
import com.medisphere.payment.domain.PaymentNotifyRequest;
import com.medisphere.payment.dto.request.PaymentNotifyRequestDTO;
import com.medisphere.payment.service.PaymentService;
import com.medisphere.payment.util.EndPoint;
import com.medisphere.payment.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Log4j2
public class PaymentController {

    private final PaymentService paymentService;
    private final ModelMapper modelMapper;

    @PostMapping(value = EndPoint.INITIATE_PAYMENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> initiatePayment(@RequestBody InitiatePaymentRequestDTO requestDTO) {
        log.info("Received request to initiate payment: {}", Utility.objectToJson(requestDTO));
        return paymentService.initiatePayment(modelMapper.map(requestDTO, InitiatePaymentRequest.class));
    }

    @PostMapping(value = EndPoint.HANDLE_NOTIFY, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleNotify(@RequestBody PaymentNotifyRequestDTO requestDTO) {
        log.info("Received payment notification for Order: {}", Utility.objectToJson(requestDTO));
        return paymentService.handleNotify(modelMapper.map(requestDTO, PaymentNotifyRequest.class));
    }

    @PostMapping(value = EndPoint.SIMULATE_SUCCESS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> simulateSuccess(@PathVariable String orderId) {
        log.info("Received request to simulate success for order: {}", orderId);
        return paymentService.simulateLocalPaymentSuccess(orderId);
    }

    @GetMapping(value = EndPoint.PAYMENT_HISTORY, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getPaymentHistory() {
        log.info("Received request to get payment history");
        return paymentService.getPaymentHistory();
    }

    @GetMapping(value = EndPoint.GET_DOCTOR_CHARGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getDoctorCharge(@PathVariable String doctorId) {
        log.info("Received request to get charges for doctor: {}", doctorId);
        return paymentService.getDoctorCharge(doctorId);
    }

    @GetMapping(value = EndPoint.GET_PAYMENT_BY_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getPaymentById(@PathVariable String orderId) {
        log.info("Received request to get payment details for order: {}", orderId);
        return paymentService.getPaymentByOrderId(orderId);
    }
}
