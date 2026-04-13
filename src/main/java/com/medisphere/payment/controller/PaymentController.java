package com.medisphere.payment.controller;

import com.medisphere.payment.domain.InitiatePaymentRequest;
import com.medisphere.payment.dto.request.InitiatePaymentRequestDTO;
import com.medisphere.payment.domain.PaymentNotifyRequest;
import com.medisphere.payment.dto.request.PaymentNotifyRequestDTO;
import com.medisphere.payment.service.PaymentService;
import com.medisphere.payment.util.EndPoint;
import com.medisphere.payment.util.PayHereHasherUtil;
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
    private final PayHereHasherUtil payHereHasherUtil;

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

    @PostMapping(value = EndPoint.GENERATE_TEST_HASH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> generateTestHash(@RequestBody PaymentNotifyRequestDTO requestDTO) {
        log.info("Generating test hash for paymentRefId: {}, amount: {}, currency: {}, statusCode: {}", 
                requestDTO.getPaymentRefId(), requestDTO.getPayhere_amount(), requestDTO.getPayhere_currency(), requestDTO.getStatus_code());
        
        String md5sig = payHereHasherUtil.generateHash(
                requestDTO.getPaymentRefId(), 
                requestDTO.getPayhere_amount(), 
                requestDTO.getPayhere_currency(), 
                requestDTO.getStatus_code()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("paymentRefId", requestDTO.getPaymentRefId());
        response.put("md5sig", md5sig);
        response.put("instructions", "Copy this md5sig and use it in your next notify request JSON");

        return ResponseEntity.ok(response);
    }
}
