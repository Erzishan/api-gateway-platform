package com.apigateway.tenant.repository;

import com.apigateway.tenant.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT k FROM ApiKey k WHERE k.tenant.id = :tenantId")
    List<ApiKey> findAllByTenantId(@Param("tenantId") UUID tenantId);

    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
}
