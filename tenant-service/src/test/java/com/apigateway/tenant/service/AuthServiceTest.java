package com.apigateway.tenant.service;

import com.apigateway.tenant.TestDataBuilder;
import com.apigateway.tenant.dto.request.LoginRequest;
import com.apigateway.tenant.dto.request.RegisterRequest;
import com.apigateway.tenant.dto.response.AuthResponse;
import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.BadRequestException;
import com.apigateway.tenant.exception.DuplicateResourceException;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.TenantRepository;
import com.apigateway.tenant.repository.UserRepository;
import com.apigateway.tenant.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        testTenant = TestDataBuilder.buildTenant();
        testUser = TestDataBuilder.buildUser(testTenant);
    }

    // ─── REGISTER TESTS ───────────────────────────────────────

    @Test
    @DisplayName("Register: success — creates tenant and user")
    void register_success() {
        // ARRANGE — set up what the mocks return
        RegisterRequest request =
                TestDataBuilder.buildRegisterRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(tenantRepository.existsBySlug(anyString()))
                .thenReturn(false);
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("hashed-password");
        when(tenantRepository.save(any(Tenant.class)))
                .thenReturn(testTenant);
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(
                any(), anyString(), anyString(), any()))
                .thenReturn("mock-jwt-token");

        // ACT — call the method being tested
        AuthResponse response = authService.register(request);

        // ASSERT — verify the result
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken())
                .isEqualTo("mock-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail())
                .isEqualTo(testUser.getEmail());

        // Verify interactions
        verify(userRepository).existsByEmail(request.getEmail());
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
        verify(kafkaProducerService)
                .publishTenantRegistered(any());
        verify(emailService)
                .sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Register: fails when email already exists")
    void register_duplicateEmail_throwsException() {
        // ARRANGE
        RegisterRequest request =
                TestDataBuilder.buildRegisterRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true); // Email already exists

        // ACT + ASSERT
        assertThatThrownBy(
                () -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered");

        // Verify tenant was NEVER saved
        verify(tenantRepository, never())
                .save(any(Tenant.class));
        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    @DisplayName("Register: fails when slug already taken")
    void register_duplicateSlug_throwsException() {
        // ARRANGE
        RegisterRequest request =
                TestDataBuilder.buildRegisterRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);
        when(tenantRepository.existsBySlug(anyString()))
                .thenReturn(true); // Slug taken

        // ACT + ASSERT
        assertThatThrownBy(
                () -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }

    // ─── LOGIN TESTS ───────────────────────────────────────────

    @Test
    @DisplayName("Login: success — returns JWT token")
    void login_success() {
        // ARRANGE
        LoginRequest request =
                TestDataBuilder.buildLoginRequest();

        when(userRepository.findByEmailWithTenant(
                request.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(
                request.getPassword(),
                testUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(
                any(), anyString(), anyString(), any()))
                .thenReturn("mock-jwt-token");

        // ACT
        AuthResponse response = authService.login(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken())
                .isEqualTo("mock-jwt-token");
        assertThat(response.getUser().getEmail())
                .isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Login: fails when email not found")
    void login_userNotFound_throwsException() {
        // ARRANGE
        LoginRequest request =
                TestDataBuilder.buildLoginRequest();

        when(userRepository.findByEmailWithTenant(
                request.getEmail()))
                .thenReturn(Optional.empty()); // User not found

        // ACT + ASSERT
        assertThatThrownBy(
                () -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(
                        "Invalid email or password");
    }

    @Test
    @DisplayName("Login: fails when password is wrong")
    void login_wrongPassword_throwsException() {
        // ARRANGE
        LoginRequest request =
                TestDataBuilder.buildLoginRequest();

        when(userRepository.findByEmailWithTenant(
                request.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(
                request.getPassword(),
                testUser.getPasswordHash()))
                .thenReturn(false); // Wrong password

        // ACT + ASSERT
        assertThatThrownBy(
                () -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(
                        "Invalid email or password");
    }

    @Test
    @DisplayName("Login: fails when account is suspended")
    void login_suspendedAccount_throwsException() {
        // ARRANGE
        LoginRequest request =
                TestDataBuilder.buildLoginRequest();

        testUser.setStatus(User.UserStatus.SUSPENDED);

        when(userRepository.findByEmailWithTenant(
                request.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(
                () -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("suspended");
    }
}