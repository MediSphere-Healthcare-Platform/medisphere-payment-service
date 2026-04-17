package com.medisphere.payment.client;

import com.medisphere.payment.client.response.AuthApiClientResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "medisphere-auth-service", fallbackFactory = MedisphereAuthClientFallbackFactory.class, dismiss404 = true)
public interface MedisphereAuthClient {

    @GetMapping("/api/v1/auth/email/{msUserId}")
    ResponseEntity<AuthApiClientResponse<String>> getEmailByMsUserId(@PathVariable("msUserId") String msUserId);
}

@Component
@Log4j2
class MedisphereAuthClientFallbackFactory implements FallbackFactory<MedisphereAuthClient> {
    @Override
    public MedisphereAuthClient create(Throwable cause) {
        return new MedisphereAuthClient() {
            @Override
            public ResponseEntity<AuthApiClientResponse<String>> getEmailByMsUserId(String msUserId) {
                log.warn("Auth service is currently unavailable. Please try again later -> getEmailByMsUserId(). Cause: {}", cause.getMessage());
                return ResponseEntity.ok(AuthApiClientResponse.<String>builder().build());
            }
        };
    }
}
