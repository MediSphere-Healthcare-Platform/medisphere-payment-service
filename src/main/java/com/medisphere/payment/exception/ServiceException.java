package com.medisphere.payment.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final String code;
    private final String description;

    public ServiceException(String code, String description) {
        super(description);
        this.code = code;
        this.description = description;
    }
}
