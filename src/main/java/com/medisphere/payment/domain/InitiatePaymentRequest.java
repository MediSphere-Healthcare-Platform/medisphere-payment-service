package com.medisphere.payment.domain;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {
    private String appointmentReferenceId;
    private String patientId;
    private String msUserId;
    private String amount;
    private String currency = "LKR";
}
