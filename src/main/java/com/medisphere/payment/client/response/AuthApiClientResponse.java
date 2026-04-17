package com.medisphere.payment.client.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthApiClientResponse<T> {
    private String status;
    private String message;
    private T data;
}
