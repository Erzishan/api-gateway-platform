package com.apigateway.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tenants",
        indexes = {
                @Index(name = "idx_tenants_slug",
                        columnList = "slug"),
                @Index(name = "idx_tenants_status",
                        columnList = "status"),
                @Index(name = "idx_tenants_plan",
                        columnList = "plan")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "stripe_id")
    private String stripeId;

    @Column(name = "quota_limit", nullable = false)
    @Builder.Default
    private Long quotaLimit = 1_000_000L;

    @Column(name = "quota_used", nullable = false)
    @Builder.Default
    private Long quotaUsed = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Plan {
        FREE, STARTER, PRO, ENTERPRISE
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}