package com.medisphere.payment.client;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.medisphere.payment.client.response.PatientByIdClientResponse;

@FeignClient(name = "medisphere-patient-service", fallbackFactory = MedispherePatientFallbackFactory.class, dismiss404 = true)
public interface MedispherePatientClient {

    @GetMapping("/patient/api/v1/getPatientById/{id}")
    ResponseEntity<PatientByIdClientResponse> getPatientById(@PathVariable("id") String id);
}

@Component
@Log4j2
class MedispherePatientFallbackFactory implements FallbackFactory<MedispherePatientClient> {

    @Override
    public MedispherePatientClient create(Throwable cause) {
        return new MedispherePatientClient() {
            @Override
            public ResponseEntity<PatientByIdClientResponse> getPatientById(String id) {
                log.warn("Doctor service is currently unavailable. Please try again later -> getDoctorById(). Cause: {}", cause.getMessage());
                return ResponseEntity.ok(PatientByIdClientResponse.builder().build());
            }
        };
    }
}
