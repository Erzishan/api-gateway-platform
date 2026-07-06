package com.apigateway.tenant.service;

import com.apigateway.tenant.dto.request.LoginRequest;
import com.apigateway.tenant.dto.request.RegisterRequest;
import com.apigateway.tenant.dto.response.AuthResponse;
import com.apigateway.tenant.dto.response.UserResponse;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.exception.DuplicateResourceException;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.TenantRepository;
import com.apigateway.tenant.repository.UserRepository;
import com.apigateway.tenant.security.JwtTokenProvider;
import com.apigateway.tenant.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Check 1: Email must not already exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        // Check 2: Generate slug and ensure it's unique
        String slug = generateSlug(request.getOrganizationName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException(
                    "Organization name already taken. Please choose another.");
        }

        // Step 1: Create and save the Tenant first
        Tenant tenant = Tenant.builder()
                .name(request.getOrganizationName())
                .slug(slug)
                .plan(Tenant.Plan.FREE)
                .status(Tenant.TenantStatus.ACTIVE)
                .quotaLimit(1_000_000L)
                .quotaUsed(0L)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Created new tenant: {} with id: {}", tenant.getName(), tenant.getId());

        // Step 2: Hash password - NEVER store plain text
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Step 3: Create and save the User linked to the Tenant
        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(User.Role.OWNER)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("Created new user: {} for tenant: {}", user.getEmail(), tenant.getId());

        // Step 4: Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId()
        );

        // Step 5: Build and return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, tenant))
                .build();
    }
     @Transactional
    public AuthResponse login(LoginRequest request) {

         log.info("Login attempt for: {}", request.getEmail());

        // Step 1: Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invalid email or password"));

         log.info("User found: {}", user.getEmail());

        // Step 2: Verify password
        if (!passwordEncoder.matches(request.getPassword(),
                user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

         log.info("Password verified");

        // Step 3: Check account is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BadRequestException(
                    "Your account has been suspended. Please contact support.");
        }


         // Step 4: Load tenant DIRECTLY from repository — avoids lazy loading issue
         Tenant tenant = tenantRepository.findById(user.getTenant().getId())
                 .orElseThrow(() -> new ResourceNotFoundException(
                         "Tenant not found"));

         log.info("Tenant loaded: {}", tenant.getName());

        // Step 5: Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId()
        );

        log.info("User logged in: {}", user.getEmail());

        // Step 6: Return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, tenant))
                .build();
    }

    // Converts org name "Acme Corp" to URL-safe slug "acme-corp"
    private String generateSlug(String organizationName) {
        return organizationName
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    // Builds a safe UserResponse (no password, no sensitive data)
    private UserResponse buildUserResponse(User user, Tenant tenant) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenantId(tenant.getId().toString())
                .tenantName(tenant.getName())
                .plan(tenant.getPlan().name())
                .build();
    }
}