package com.apigateway.tenant.service;

import com.apigateway.tenant.dto.response.TenantResponse;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.repository.TenantRepository;
import com.apigateway.tenant.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;

    // Result cached in Redis for 5 minutes
    // Cache key = tenant's UUID
    @Cacheable(value = "tenants",
            key = "#root.target.securityUtils" +
                    ".getCurrentUser().tenant.id")
    @Transactional(readOnly = true)
    public TenantResponse getCurrentTenant() {
        User currentUser = securityUtils.getCurrentUser();
        Tenant tenant = currentUser.getTenant();
        log.debug("Cache MISS — loading tenant from DB: {}",
                tenant.getId());
        return buildTenantResponse(tenant);
    }

    // When tenant is updated, remove it from cache
    @CacheEvict(value = "tenants",
            key = "#root.target.securityUtils" +
                    ".getCurrentUser().tenant.id")
    @Transactional
    public TenantResponse updateTenant(String newName) {
        User currentUser = securityUtils.getCurrentUser();
        Tenant tenant = currentUser.getTenant();

        if (currentUser.getRole() != User.Role.OWNER &&
                currentUser.getRole() != User.Role.ADMIN) {
            throw new BadRequestException(
                    "You do not have permission to update " +
                            "organization details");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new BadRequestException(
                    "Organization name cannot be empty");
        }

        tenant.setName(newName.trim());
        tenant = tenantRepository.save(tenant);

        log.info("Tenant updated: {} — cache evicted",
                tenant.getId());

        return buildTenantResponse(tenant);
    }

    private TenantResponse buildTenantResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .plan(tenant.getPlan().name())
                .status(tenant.getStatus().name())
                .quotaLimit(tenant.getQuotaLimit())
                .quotaUsed(tenant.getQuotaUsed())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}