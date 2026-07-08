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

import java.util.List;
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
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        return ResponseEntity.ok(apiKeyService.listApiKeys());
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
