package com.medisphere.payment.client;

import com.medisphere.payment.client.request.NotificationClientRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "medisphere-notification-service", fallbackFactory = MedisphereNotificationClientFallbackFactory.class)
public interface MedisphereNotificationClient {

    @PostMapping("/api/v1/notifications")
    ResponseEntity<Object> createNotification(@RequestBody NotificationClientRequest notificationClientRequest);
}

@Component
@Log4j2
class MedisphereNotificationClientFallbackFactory implements FallbackFactory<MedisphereNotificationClient> {
    @Override
    public MedisphereNotificationClient create(Throwable cause) {
        return new MedisphereNotificationClient() {
            @Override
            public ResponseEntity<Object> createNotification(NotificationClientRequest notificationClientRequest) {
                log.warn("Notification service is currently unavailable. Fallback triggered. Cause: {}", cause.getMessage());
                return ResponseEntity.ok().build();
            }
        };
    }
}
