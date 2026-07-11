package com.apigateway.tenant.repository;

import com.apigateway.tenant.document.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository
        extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByTenantIdOrderByOccurredAtDesc(
            String tenantId, Pageable pageable);

    List<AuditLog> findByTenantIdOrderByOccurredAtDesc(
            String tenantId);

    Page<AuditLog> findByTenantIdAndActionOrderByOccurredAtDesc(
            String tenantId, String action, Pageable pageable);
}