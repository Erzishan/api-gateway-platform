package com.apigateway.tenant.controller;

import com.apigateway.tenant.dto.request.CreateApiKeyRequest;
import com.apigateway.tenant.dto.response.ApiKeyResponse;
import com.apigateway.tenant.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.apigateway.tenant.dto.response.PagedResponse;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    // OWNER, ADMIN, DEVELOPER can create keys
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DEVELOPER')")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // All authenticated users can list keys
    @GetMapping
    public ResponseEntity<PagedResponse<ApiKeyResponse>> listApiKeys(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false")
            boolean activeOnly) {
        return ResponseEntity.ok(
                apiKeyService.listApiKeys(page, size, activeOnly));
    }

    // All authenticated users can view a specific key
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> getApiKey(
            @PathVariable UUID id) {
        return ResponseEntity.ok(apiKeyService.getApiKey(id));
    }

    // Only OWNER and ADMIN can revoke keys
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable UUID id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
