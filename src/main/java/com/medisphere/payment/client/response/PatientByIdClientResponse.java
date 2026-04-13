package com.medisphere.payment.client.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientByIdClientResponse {
    private int code;
    private String message;
    private PatientClientResponse data;
}
