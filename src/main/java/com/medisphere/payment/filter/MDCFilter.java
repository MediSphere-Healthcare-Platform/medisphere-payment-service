package com.medisphere.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class MDCFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = UUID.randomUUID().toString();
            MDC.put("REQ_ID", requestId);
            MDC.put("NAME", applicationName != null ? applicationName.toUpperCase() : "PAYMENT-SERVICE");

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
