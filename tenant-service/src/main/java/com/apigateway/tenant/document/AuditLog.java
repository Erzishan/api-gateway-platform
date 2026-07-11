package com.apigateway.tenant.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_tenant_time",
                def = "{'tenantId': 1, 'occurredAt': -1}"
        ),
        @CompoundIndex(
                name = "idx_tenant_action",
                def = "{'tenantId': 1, 'action': 1}"
        )
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String action;
    private String resourceType;
    private String resourceId;
    private String performedBy;
    private String details;

    @Indexed
    private LocalDateTime occurredAt;
}