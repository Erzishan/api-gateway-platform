package com.apigateway.tenant.service;

import com.apigateway.tenant.document.AuditLog;
import com.apigateway.tenant.event.AuditEvent;
import com.apigateway.tenant.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // Listens to Kafka audit.events topic
    // Runs in background — never blocks main thread
    @KafkaListener(
            topics = "audit.events",
            groupId = "audit-service-group"
    )
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(event.getTenantId())
                    .action(event.getAction())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .performedBy(event.getPerformedBy())
                    .details(event.getDetails())
                    .occurredAt(event.getOccurredAt())
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log saved: {} by {}",
                    event.getAction(),
                    event.getPerformedBy());

        } catch (Exception e) {
            log.error("Failed to save audit log: {}",
                    e.getMessage());
        }
    }

    public List<AuditLog> getAuditLogs(String tenantId) {
        return auditLogRepository
                .findByTenantIdOrderByOccurredAtDesc(tenantId);
    }
}
