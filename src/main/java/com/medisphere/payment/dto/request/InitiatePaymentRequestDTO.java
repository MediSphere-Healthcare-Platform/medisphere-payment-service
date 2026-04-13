package com.medisphere.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequestDTO {
    private String appointmentReferenceId;
    private String patientId;
    private String msUserId;
    private String amount;
    private String currency;
}
