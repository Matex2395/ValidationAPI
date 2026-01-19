package com.fisa.validationapi.infrastructure.adapters.output.feign.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String recipient;
    private String subject;
    private String body;
    private String originService;
}
