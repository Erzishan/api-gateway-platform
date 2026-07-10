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
public class AuditEvent {

    private String action;
    private String resourceType;
    private String resourceId;
    private String tenantId;
    private String performedBy;
    private String details;
    private LocalDateTime occurredAt;
}