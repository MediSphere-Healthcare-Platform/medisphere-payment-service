package com.medisphere.payment.service;

import com.medisphere.payment.domain.InitiatePaymentRequest;
import com.medisphere.payment.domain.PaymentNotifyRequest;
import org.springframework.http.ResponseEntity;

public interface PaymentService {
    ResponseEntity<Object> initiatePayment(InitiatePaymentRequest request);

    ResponseEntity<Object> handleNotify(PaymentNotifyRequest request);
}
