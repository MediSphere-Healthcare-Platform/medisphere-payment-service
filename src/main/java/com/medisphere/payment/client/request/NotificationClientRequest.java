package com.medisphere.payment.client.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationClientRequest {
    private String userId;
    private String userRole;
    private String message;
    private String title;
    private String channel;
    private String relatedId;
    private boolean isBroadcast;
}
