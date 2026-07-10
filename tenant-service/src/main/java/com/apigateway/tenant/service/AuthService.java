package com.apigateway.tenant.service;

import com.apigateway.tenant.dto.request.LoginRequest;
import com.apigateway.tenant.dto.request.RegisterRequest;
import com.apigateway.tenant.dto.response.AuthResponse;
import com.apigateway.tenant.dto.response.UserResponse;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.event.AuditEvent;
import com.apigateway.tenant.event.TenantRegisteredEvent;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.exception.DuplicateResourceException;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.TenantRepository;
import com.apigateway.tenant.repository.UserRepository;
import com.apigateway.tenant.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaProducerService kafkaProducerService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        String slug = generateSlug(request.getOrganizationName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException(
                    "Organization name already taken.");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getOrganizationName())
                .slug(slug)
                .plan(Tenant.Plan.FREE)
                .status(Tenant.TenantStatus.ACTIVE)
                .quotaLimit(1_000_000L)
                .quotaUsed(0L)
                .build();

        tenant = tenantRepository.save(tenant);

        String hashedPassword = passwordEncoder.encode(
                request.getPassword());

        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(User.Role.OWNER)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        log.info("New tenant registered: {} by {}",
                tenant.getId(), user.getEmail());

        // Publish event to Kafka — fire and forget
        kafkaProducerService.publishTenantRegistered(
                TenantRegisteredEvent.builder()
                        .tenantId(tenant.getId().toString())
                        .tenantName(tenant.getName())
                        .ownerEmail(user.getEmail())
                        .plan(tenant.getPlan().name())
                        .registeredAt(LocalDateTime.now())
                        .build()
        );

        // Publish audit event
        kafkaProducerService.publishAuditEvent(
                AuditEvent.builder()
                        .action("TENANT_REGISTERED")
                        .resourceType("TENANT")
                        .resourceId(tenant.getId().toString())
                        .tenantId(tenant.getId().toString())
                        .performedBy(user.getEmail())
                        .details("New tenant registered: " +
                                tenant.getName())
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        // Send welcome email async — does not block response
        emailService.sendWelcomeEmail(
                user.getEmail(),
                tenant.getName());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId()
        );

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

        User user = userRepository
                .findByEmailWithTenant(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(),
                user.getPasswordHash())) {
            throw new BadRequestException(
                    "Invalid email or password");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BadRequestException(
                    "Your account has been suspended.");
        }

        Tenant tenant = user.getTenant();

        // Publish audit event for login
        kafkaProducerService.publishAuditEvent(
                AuditEvent.builder()
                        .action("USER_LOGIN")
                        .resourceType("USER")
                        .resourceId(user.getId().toString())
                        .tenantId(tenant.getId().toString())
                        .performedBy(user.getEmail())
                        .details("User logged in successfully")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        log.info("User logged in: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, tenant))
                .build();
    }

    private String generateSlug(String organizationName) {
        return organizationName
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private UserResponse buildUserResponse(User user,
                                           Tenant tenant) {
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