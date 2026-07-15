package com.apigateway.tenant;

import com.apigateway.tenant.dto.request.LoginRequest;
import com.apigateway.tenant.dto.request.RegisterRequest;
import com.apigateway.tenant.entity.ApiKey;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataBuilder {

    // Creates a test Tenant object
    public static Tenant buildTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setPlan(Tenant.Plan.FREE);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setQuotaLimit(1_000_000L);
        tenant.setQuotaUsed(0L);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        return tenant;
    }

    // Creates a test User linked to a Tenant
    public static User buildUser(Tenant tenant) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTenant(tenant);
        user.setEmail("test@testcompany.com");
        user.setPasswordHash(
                "$2a$10$test.hash.for.testing.purposes.only");
        user.setRole(User.Role.OWNER);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    // Creates a test ApiKey
    public static ApiKey buildApiKey(Tenant tenant) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID());
        apiKey.setTenant(tenant);
        apiKey.setName("Test Key");
        apiKey.setKeyPrefix("gw_test");
        apiKey.setKeyHash("test-hash-value");
        apiKey.setRateLimit(100);
        apiKey.setRateWindowSeconds(60);
        apiKey.setCreatedAt(LocalDateTime.now());
        return apiKey;
    }

    // Creates a valid RegisterRequest
    public static RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Test Company");
        request.setEmail("test@testcompany.com");
        request.setPassword("testpassword123");
        request.setFullName("Test User");
        return request;
    }

    // Creates a valid LoginRequest
    public static LoginRequest buildLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@testcompany.com");
        request.setPassword("testpassword123");
        return request;
    }
}