package com.apigateway.tenant.controller;

import com.apigateway.tenant.dto.request.LoginRequest;
import com.apigateway.tenant.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.TimeZone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        registry.add("spring.datasource.url",
                postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
                postgres::getUsername);
        registry.add("spring.datasource.password",
                postgres::getPassword);
        // Disable Kafka for integration tests
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure" +
                        ".kafka.KafkaAutoConfiguration");
        // Disable MongoDB for integration tests
        registry.add("spring.data.mongodb.auto-index-creation",
                () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Register: POST /api/v1/auth/register returns 201")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Integration Test Co");
        request.setEmail("integration@testco.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.user.email")
                        .value("integration@testco.com"))
                .andExpect(jsonPath("$.user.role")
                        .value("OWNER"));
    }

    @Test
    @DisplayName("Register: duplicate email returns 409")
    void register_duplicateEmail_returns409()
            throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Duplicate Co");
        request.setEmail("duplicate@testco.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Conflict"));
    }

    @Test
    @DisplayName("Register: invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Test Co");
        request.setEmail("not-an-email");
        request.setPassword("password123");
        request.setFullName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Validation Failed"));
    }

    @Test
    @DisplayName("Login: valid credentials returns 200 with token")
    void login_validCredentials_returns200() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setOrganizationName("Login Test Co");
        registerRequest.setEmail("login@testco.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@testco.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email")
                        .value("login@testco.com"));
    }

    @Test
    @DisplayName("Login: wrong password returns 400")
    void login_wrongPassword_returns400() throws Exception {
        // First register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setOrganizationName("Wrong Pass Co");
        registerRequest.setEmail("wrongpass@testco.com");
        registerRequest.setPassword("correctpassword");
        registerRequest.setFullName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@testco.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    @Test
    @DisplayName("Protected endpoint: no token returns 403")
    void protectedEndpoint_noToken_returns403()
            throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me"))
                .andExpect(status().isForbidden());
    }
}