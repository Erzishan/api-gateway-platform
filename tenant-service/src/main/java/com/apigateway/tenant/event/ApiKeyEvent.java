package com.apigateway.tenant.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyEvent {

    private String eventType;
    private String apiKeyId;
    private String apiKeyName;
    private String tenantId;
    private String performedByEmail;
    private LocalDateTime occurredAt;
}
