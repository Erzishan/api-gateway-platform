package com.apigateway.tenant.repository;

import com.apigateway.tenant.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository
        extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByTenantIdOrderByOccurredAtDesc(
            String tenantId);
}
