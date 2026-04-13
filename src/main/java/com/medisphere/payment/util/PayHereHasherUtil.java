package com.medisphere.payment.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility to generate PayHere MD5 hashes for testing purposes.
 * This class logs a valid MD5 signature for a sample payment reference ID on startup.
 */
@Component
@Log4j2
public class PayHereHasherUtil {

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @PostConstruct
    public void init() {
        String testPaymentRefId = "PAY-DATA-TEST-001";
        String md5sig = generateHash(testPaymentRefId, "2000", "LKR", "2");

        log.info("==========================================================");
        log.info("PAYHERE TEST HASH GENERATED (@PostConstruct)");
        log.info("Payment Reference ID : {}", testPaymentRefId);
        log.info("Status Code          : {}", "2");
        log.info("Valid MD5 Signature  : {}", md5sig);
        log.info("==========================================================");
    }

    public String generateHash(String paymentRefId, String amount, String currency, String statusCode) {
        String secretHash = md5(merchantSecret).toUpperCase();
        String mainString = merchantId + paymentRefId + amount + currency + statusCode + secretHash;
        return md5(mainString).toUpperCase();
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
            return "";
        }
    }
}
