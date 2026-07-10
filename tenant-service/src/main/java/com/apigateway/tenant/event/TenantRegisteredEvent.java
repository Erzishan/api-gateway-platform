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
public class TenantRegisteredEvent {

    private String tenantId;
    private String tenantName;
    private String ownerEmail;
    private String plan;
    private LocalDateTime registeredAt;
}