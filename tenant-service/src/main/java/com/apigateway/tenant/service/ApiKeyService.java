package com.apigateway.tenant.service;

import com.apigateway.tenant.dto.request.CreateApiKeyRequest;
import com.apigateway.tenant.dto.response.ApiKeyResponse;
import com.apigateway.tenant.entity.ApiKey;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.ApiKeyRepository;
import com.apigateway.tenant.util.ApiKeyGenerator;
import com.apigateway.tenant.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apigateway.tenant.dto.response.PagedResponse;
import com.apigateway.tenant.constants.AppConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final SecurityUtils securityUtils;

    @Transactional
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Only OWNER and ADMIN can create API keys
        if (currentUser.getRole() == User.Role.VIEWER) {
            throw new BadRequestException(
                    "Viewers cannot create API keys");
        }

        // Generate the full key - shown to user ONCE only
        String fullKey = apiKeyGenerator.generateKey();
        String keyPrefix = apiKeyGenerator.extractPrefix(fullKey);
        String keyHash = apiKeyGenerator.hashKey(fullKey);

        // Build and save the API key entity
        ApiKey apiKey = ApiKey.builder()
                .tenant(currentUser.getTenant())
                .name(request.getName())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .rateLimit(request.getRateLimit())
                .rateWindowSeconds(request.getRateWindowSeconds())
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        log.info("API key created: {} for tenant: {}",
                apiKey.getId(), currentUser.getTenant().getId());

        // Build response WITH the full key — only time we return it
        ApiKeyResponse response = buildApiKeyResponse(apiKey);
        response.setFullKey(fullKey);
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApiKeyResponse> listApiKeys(
            int page, int size, boolean activeOnly) {

        User currentUser = securityUtils.getCurrentUser();
        UUID tenantId = currentUser.getTenant().getId();

        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ApiKey> apiKeyPage =
                apiKeyRepository.findByTenantIdWithFilter(
                        tenantId, activeOnly, pageable);

        List<ApiKeyResponse> content = apiKeyPage
                .getContent()
                .stream()
                .map(this::buildApiKeyResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ApiKeyResponse>builder()
                .content(content)
                .page(apiKeyPage.getNumber())
                .size(apiKeyPage.getSize())
                .totalElements(apiKeyPage.getTotalElements())
                .totalPages(apiKeyPage.getTotalPages())
                .first(apiKeyPage.isFirst())
                .last(apiKeyPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKey(UUID keyId) {
        User currentUser = securityUtils.getCurrentUser();
        ApiKey apiKey = findApiKeyForCurrentTenant(keyId, currentUser);
        return buildApiKeyResponse(apiKey);
    }

    @Transactional
    public void revokeApiKey(UUID keyId) {
        User currentUser = securityUtils.getCurrentUser();

        // Only OWNER and ADMIN can revoke keys
        if (currentUser.getRole() == User.Role.VIEWER ||
                currentUser.getRole() == User.Role.DEVELOPER) {
            throw new BadRequestException(
                    "You do not have permission to revoke API keys");
        }

        ApiKey apiKey = findApiKeyForCurrentTenant(keyId, currentUser);

        if (apiKey.isRevoked()) {
            throw new BadRequestException(
                    "This API key is already revoked");
        }

        apiKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        log.info("API key revoked: {} by user: {}",
                keyId, currentUser.getEmail());
    }

    // Finds an API key and verifies it belongs to the current tenant
    private ApiKey findApiKeyForCurrentTenant(
            UUID keyId, User currentUser) {

        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "API key", "id", keyId));

        // Tenant isolation check — critical security control
        if (!apiKey.getTenant().getId()
                .equals(currentUser.getTenant().getId())) {
            throw new ResourceNotFoundException(
                    "API key", "id", keyId);
        }

        return apiKey;
    }

    private ApiKeyResponse buildApiKeyResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .rateLimit(apiKey.getRateLimit())
                .rateWindowSeconds(apiKey.getRateWindowSeconds())
                .active(apiKey.isActive())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .fullKey(null)
                .build();
    }
}