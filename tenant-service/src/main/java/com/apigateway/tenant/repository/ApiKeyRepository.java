package com.apigateway.tenant.repository;

import com.apigateway.tenant.entity.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository
        extends JpaRepository<ApiKey, UUID> {

    // Paginated version — returns one page at a time
    @Query("SELECT k FROM ApiKey k " +
            "WHERE k.tenant.id = :tenantId " +
            "ORDER BY k.createdAt DESC")
    Page<ApiKey> findByTenantIdPaged(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    // Non-paginated — for internal use only
    @Query("SELECT k FROM ApiKey k WHERE k.tenant.id = :tenantId")
    List<ApiKey> findAllByTenantId(
            @Param("tenantId") UUID tenantId);

    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    // Count active keys for a tenant
    @Query("SELECT COUNT(k) FROM ApiKey k " +
            "WHERE k.tenant.id = :tenantId " +
            "AND k.revokedAt IS NULL")
    long countActiveKeysByTenantId(
            @Param("tenantId") UUID tenantId);

    @Query("SELECT k FROM ApiKey k " +
            "WHERE k.tenant.id = :tenantId " +
            "AND (:activeOnly = false OR k.revokedAt IS NULL) " +
            "ORDER BY k.createdAt DESC")
    Page<ApiKey> findByTenantIdWithFilter(
            @Param("tenantId") UUID tenantId,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);
}