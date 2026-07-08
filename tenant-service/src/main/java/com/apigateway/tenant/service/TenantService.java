package com.apigateway.tenant.service;

import com.apigateway.tenant.dto.response.TenantResponse;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.repository.TenantRepository;
import com.apigateway.tenant.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;

    // Get the current user's tenant profile
    @Transactional(readOnly = true)
    public TenantResponse getCurrentTenant() {
        User currentUser = securityUtils.getCurrentUser();
        Tenant tenant = currentUser.getTenant();
        return buildTenantResponse(tenant);
    }

    // Update tenant name
    @Transactional
    public TenantResponse updateTenant(String newName) {
        User currentUser = securityUtils.getCurrentUser();
        Tenant tenant = currentUser.getTenant();

        // Only OWNER can update tenant details
        if (currentUser.getRole() != User.Role.OWNER &&
                currentUser.getRole() != User.Role.ADMIN) {
            throw new BadRequestException(
                    "You do not have permission to update organization details");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new BadRequestException("Organization name cannot be empty");
        }

        tenant.setName(newName.trim());
        tenant = tenantRepository.save(tenant);

        log.info("Tenant updated: {} by user: {}",
                tenant.getId(), currentUser.getEmail());

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