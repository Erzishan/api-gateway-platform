package com.apigateway.tenant.controller;

import com.apigateway.tenant.dto.request.UpdateTenantRequest;
import com.apigateway.tenant.dto.response.TenantResponse;
import com.apigateway.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;

    // Any authenticated user can view their tenant
    @GetMapping("/me")
    public ResponseEntity<TenantResponse> getCurrentTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    // Only OWNER or ADMIN can update tenant
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<TenantResponse> updateTenant(
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(
                tenantService.updateTenant(request.getName()));
    }
}
