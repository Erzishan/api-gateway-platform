package com.apigateway.tenant.service;

import com.apigateway.tenant.TestDataBuilder;
import com.apigateway.tenant.dto.request.CreateApiKeyRequest;
import com.apigateway.tenant.dto.response.ApiKeyResponse;
import com.apigateway.tenant.dto.response.PagedResponse;
import com.apigateway.tenant.entity.ApiKey;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.ApiKeyRepository;
import com.apigateway.tenant.util.ApiKeyGenerator;
import com.apigateway.tenant.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Unit Tests")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private Tenant testTenant;
    private User testUser;
    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        testTenant = TestDataBuilder.buildTenant();
        testUser = TestDataBuilder.buildUser(testTenant);
        testApiKey = TestDataBuilder.buildApiKey(testTenant);

        // Most tests need the current user
        when(securityUtils.getCurrentUser())
                .thenReturn(testUser);
    }

    @Test
    @DisplayName("Create API key: success")
    void createApiKey_success() {
        // ARRANGE
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("My Test Key");
        request.setRateLimit(100);
        request.setRateWindowSeconds(60);

        String fullKey = "gw_abc123xyz";
        when(apiKeyGenerator.generateKey()).thenReturn(fullKey);
        when(apiKeyGenerator.extractPrefix(fullKey))
                .thenReturn("gw_abc123");
        when(apiKeyGenerator.hashKey(fullKey))
                .thenReturn("hashed-value");
        when(apiKeyRepository.save(any(ApiKey.class)))
                .thenReturn(testApiKey);

        // ACT
        ApiKeyResponse response =
                apiKeyService.createApiKey(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getFullKey()).isEqualTo(fullKey);
        assertThat(response.isActive()).isTrue();
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("Create API key: VIEWER cannot create keys")
    void createApiKey_viewerRole_throwsException() {
        // ARRANGE — set user role to VIEWER
        testUser.setRole(User.Role.VIEWER);

        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("My Key");

        // ACT + ASSERT
        assertThatThrownBy(
                () -> apiKeyService.createApiKey(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(
                        "Viewers cannot create API keys");

        // Key should never be saved
        verify(apiKeyRepository, never())
                .save(any(ApiKey.class));
    }

    @Test
    @DisplayName("List API keys: returns paginated results")
    void listApiKeys_returnsPaginatedResults() {
        // ARRANGE
        List<ApiKey> keys = List.of(testApiKey);
        PageImpl<ApiKey> page = new PageImpl<>(keys);

        when(apiKeyRepository.findByTenantIdWithFilter(
                any(UUID.class),
                anyBoolean(),
                any(Pageable.class)))
                .thenReturn(page);

        // ACT
        PagedResponse<ApiKeyResponse> response =
                apiKeyService.listApiKeys(0, 20, false);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Revoke API key: success")
    void revokeApiKey_success() {
        // ARRANGE
        when(apiKeyRepository.findById(testApiKey.getId()))
                .thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class)))
                .thenReturn(testApiKey);

        // ACT
        apiKeyService.revokeApiKey(testApiKey.getId());

        // ASSERT — verify key was saved with revokedAt set
        verify(apiKeyRepository).save(argThat(key ->
                key.getRevokedAt() != null));
    }

    @Test
    @DisplayName("Revoke API key: cannot revoke already revoked key")
    void revokeApiKey_alreadyRevoked_throwsException() {
        // ARRANGE — set key as already revoked
        testApiKey.setRevokedAt(LocalDateTime.now());

        when(apiKeyRepository.findById(testApiKey.getId()))
                .thenReturn(Optional.of(testApiKey));

        // ACT + ASSERT
        assertThatThrownBy(
                () -> apiKeyService.revokeApiKey(
                        testApiKey.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already revoked");
    }

    @Test
    @DisplayName("Get API key: tenant isolation enforced")
    void getApiKey_differentTenant_throwsException() {
        // ARRANGE — key belongs to a different tenant
        Tenant otherTenant = TestDataBuilder.buildTenant();
        otherTenant.setId(UUID.randomUUID());
        testApiKey.setTenant(otherTenant);

        when(apiKeyRepository.findById(testApiKey.getId()))
                .thenReturn(Optional.of(testApiKey));

        // ACT + ASSERT
        assertThatThrownBy(
                () -> apiKeyService.getApiKey(
                        testApiKey.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}