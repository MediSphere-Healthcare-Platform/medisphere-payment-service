package com.medisphere.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayHereDetailsResponse {
    private String merchant_id;
    private String order_id;
    private String items;
    private String currency;
    private String amount;
    private String hash;
    private String first_name;
    private String last_name;
    private String email;
    private String phone;
    private String address;
    private String return_url;
    private String cancel_url;
    private String notify_url;
}
