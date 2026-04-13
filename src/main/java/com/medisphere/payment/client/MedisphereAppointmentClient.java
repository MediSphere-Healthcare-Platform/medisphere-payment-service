package com.medisphere.payment.client;

import com.medisphere.payment.client.request.AppointmentStatusChangeClientRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "medisphere-appointment-service", fallbackFactory = MedisphereAppointmentFallbackFactory.class, dismiss404 = true)
public interface MedisphereAppointmentClient {

    @PutMapping("/api/v1/appointments/appointmentStatusChange")
    ResponseEntity<Object> updateAppointmentStatus(@RequestBody AppointmentStatusChangeClientRequest request);
}

@Component
@Log4j2
class MedisphereAppointmentFallbackFactory implements FallbackFactory<MedisphereAppointmentClient> {
    @Override
    public MedisphereAppointmentClient create(Throwable cause) {
        return new MedisphereAppointmentClient() {
            @Override
            public ResponseEntity<Object> updateAppointmentStatus(AppointmentStatusChangeClientRequest request) {
                log.warn("Appointment service is currently unavailable. Please try again later. Cause: {}", cause.getMessage());
                return ResponseEntity.ok(null);
            }
        };
    }
}