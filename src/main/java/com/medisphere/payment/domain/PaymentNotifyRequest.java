package com.medisphere.payment.domain;

import lombok.Data;

@Data
public class PaymentNotifyRequest {
    private String merchant_id;
    private String paymentRefId;
    private String payhere_payment_id;
    private String payhere_amount;
    private String payhere_currency;
    private String status_code;
    private String md5sig;
    private String custom_1;
    private String custom_2;
    private String method;
    private String status_message;
    private String card_holder_name;
    private String card_no;
    private String card_expiry;
}
