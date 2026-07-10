package com.apigateway.tenant.service;

import com.apigateway.tenant.constants.KafkaTopics;
import com.apigateway.tenant.event.ApiKeyEvent;
import com.apigateway.tenant.event.AuditEvent;
import com.apigateway.tenant.event.TenantRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTenantRegistered(
            TenantRegisteredEvent event) {
        kafkaTemplate.send(
                KafkaTopics.TENANT_REGISTERED,
                event.getTenantId(),
                event);
        log.info("Published tenant.registered event for: {}",
                event.getTenantId());
    }

    public void publishApiKeyEvent(ApiKeyEvent event) {
        kafkaTemplate.send(
                KafkaTopics.API_KEY_CREATED,
                event.getTenantId(),
                event);
        log.info("Published apikey event: {} for tenant: {}",
                event.getEventType(),
                event.getTenantId());
    }

    public void publishAuditEvent(AuditEvent event) {
        kafkaTemplate.send(
                KafkaTopics.AUDIT_EVENTS,
                event.getTenantId(),
                event);
    }
}
