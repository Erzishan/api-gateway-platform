package com.apigateway.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "api_keys",
        indexes = {
                @Index(name = "idx_apikeys_tenant_id",
                        columnList = "tenant_id"),
                @Index(name = "idx_apikeys_key_prefix",
                        columnList = "key_prefix"),
                @Index(name = "idx_apikeys_revoked_at",
                        columnList = "revoked_at"),
                @Index(name = "idx_apikeys_tenant_active",
                        columnList = "tenant_id, revoked_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 8)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(name = "rate_limit")
    private Integer rateLimit;

    @Column(name = "rate_window_seconds")
    private Integer rateWindowSeconds;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null &&
                LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isRevoked() && !isExpired();
    }
}