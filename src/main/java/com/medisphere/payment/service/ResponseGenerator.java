package com.medisphere.payment.service;

import org.springframework.http.ResponseEntity;

public interface ResponseGenerator {

    ResponseEntity<Object> generateResponse(String code, String description, Object data);

}
