package com.medisphere.payment.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Log4j2
public class PayHereHasherUtil {

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    public String generateCheckoutHash(String orderId, String amount, String currency) {

        // Ensure clean values
        String cleanAmount = amount.trim();
        String cleanCurrency = currency.trim().toUpperCase();

        String secretHash = md5(merchantSecret).toUpperCase();

        String raw = merchantId.trim() + orderId.trim() + cleanAmount + cleanCurrency + secretHash;

        String finalHash = md5(raw).toUpperCase();

        // Debug log (VERY IMPORTANT)
        log.info("=== PAYHERE CHECKOUT HASH DEBUG ===");
        log.info("merchantId: [{}]", merchantId);
        log.info("orderId   : [{}]", orderId);
        log.info("amount    : [{}]", cleanAmount);
        log.info("currency  : [{}]", cleanCurrency);
        log.info("hash      : [{}]", finalHash);
        log.info("===================================");

        return finalHash;
    }

    public String generateNotifyHash(String orderId, String amount, String currency, String statusCode) {

        String cleanAmount = amount.trim();
        String cleanCurrency = currency.trim().toUpperCase();

        String secretHash = md5(merchantSecret).toUpperCase();

        String raw = merchantId.trim() + orderId.trim() + cleanAmount + cleanCurrency + statusCode + secretHash;

        return md5(raw).toUpperCase();
    }

    private String md5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(message.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error generating MD5 hash", e);
            throw new RuntimeException(e);
        }
    }
}