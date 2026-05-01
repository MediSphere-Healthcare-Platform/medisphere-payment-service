package com.medisphere.payment.util;

public class EndPoint {
    public static final String INITIATE_PAYMENT = "payment/initiate";
    public static final String HANDLE_NOTIFY = "payment/notify";
    public static final String SIMULATE_SUCCESS = "payment/simulate-success/{orderId}";
    public static final String PAYMENT_HISTORY = "payment/history";
    public static final String GET_DOCTOR_CHARGE = "payment/doctor-charge/{doctorId}";
    public static final String GET_PAYMENT_BY_ID = "payment/details/{orderId}";
}
