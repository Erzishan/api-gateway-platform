package com.apigateway.tenant.repository;

import com.apigateway.tenant.entity.Tenant;
import com.apigateway.tenant.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

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
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant savedTenant;

    @BeforeEach
    void setUp() {
        // Create a real tenant in the test database
        Tenant tenant = new Tenant();
        tenant.setName("Test Company");
        tenant.setSlug("test-company-repo");
        tenant.setPlan(Tenant.Plan.FREE);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setQuotaLimit(1_000_000L);
        tenant.setQuotaUsed(0L);
        savedTenant = tenantRepository.save(tenant);
    }

    @Test
    @DisplayName("findByEmail: finds existing user")
    void findByEmail_existingUser_returnsUser() {
        // ARRANGE — save a real user to the test database
        User user = new User();
        user.setTenant(savedTenant);
        user.setEmail("repo@test.com");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.OWNER);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        // ACT
        Optional<User> found =
                userRepository.findByEmail("repo@test.com");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getEmail())
                .isEqualTo("repo@test.com");
        assertThat(found.get().getRole())
                .isEqualTo(User.Role.OWNER);
    }

    @Test
    @DisplayName("findByEmail: returns empty for unknown email")
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> found =
                userRepository.findByEmail("nobody@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail: returns true for existing email")
    void existsByEmail_existingEmail_returnsTrue() {
        // ARRANGE
        User user = new User();
        user.setTenant(savedTenant);
        user.setEmail("exists@test.com");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.OWNER);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        // ACT + ASSERT
        assertThat(userRepository
                .existsByEmail("exists@test.com")).isTrue();
        assertThat(userRepository
                .existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    @DisplayName("findByEmailWithTenant: loads user and tenant together")
    void findByEmailWithTenant_loadsRelationship() {
        // ARRANGE
        User user = new User();
        user.setTenant(savedTenant);
        user.setEmail("join@test.com");
        user.setPasswordHash("hashed");
        user.setRole(User.Role.OWNER);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        // ACT
        Optional<User> found =
                userRepository.findByEmailWithTenant(
                        "join@test.com");

        // ASSERT
        assertThat(found).isPresent();
        // Tenant should be loaded (no LazyInitializationException)
        assertThat(found.get().getTenant()).isNotNull();
        assertThat(found.get().getTenant().getName())
                .isEqualTo("Test Company");
    }
}