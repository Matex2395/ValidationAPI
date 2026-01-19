package com.fisa.validationapi.infrastructure.adapters.output.feign;

import com.fisa.validationapi.infrastructure.adapters.output.feign.dtos.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Define la URL en application.yml o ponla directa por ahora: url = "http://localhost:8098"
@FeignClient(name = "notification-api", url = "http://localhost:8098")
public interface NotificationClient {

    @PostMapping(value = "/api/v1/notifications/email", consumes = MediaType.APPLICATION_JSON_VALUE)
    void sendEmail(@RequestBody NotificationRequest request);
}