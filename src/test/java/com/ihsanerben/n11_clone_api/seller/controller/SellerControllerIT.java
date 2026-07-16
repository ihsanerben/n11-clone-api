package com.ihsanerben.n11_clone_api.seller.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
import com.ihsanerben.n11_clone_api.seller.repository.SellerRepository;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.entity.UserRole;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SellerControllerIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.cors.allowed-origin", () -> "http://localhost:5173");
    registry.add("app.auth.jwt-secret", () -> "integration-test-secret-key-at-least-32-characters");
    registry.add("app.auth.access-expiration-ms", () -> "1200000");
    registry.add("app.auth.refresh-expiration-days", () -> "14");
    registry.add("app.auth.verification-expiration-hours", () -> "24");
    registry.add("app.auth.password-reset-expiration-minutes", () -> "60");
    registry.add("app.auth.refresh-cookie-name", () -> "refreshToken");
    registry.add("app.auth.cookie-secure", () -> "false");
    registry.add("app.email.api-key", () -> "test-key");
    registry.add("app.email.from", () -> "test@example.com");
    registry.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired SellerRepository sellers;
  @Autowired TokenService tokenService;
  @MockitoBean ResendEmailService emailService;

  @Test
  void shouldApplyApproveAndEnforceRoleBoundaries() throws Exception {
    User applicant = saveUser("applicant", UserRole.USER);
    User admin = saveUser("admin", UserRole.ADMIN);
    String userToken = tokenService.createAccessToken(applicant);
    String adminToken = tokenService.createAccessToken(admin);

    mockMvc
        .perform(
            post("/api/sellers/apply")
                .header("Authorization", "Bearer " + userToken)
                .contentType(APPLICATION_JSON)
                .content("{\"storeName\":\"My Store\",\"description\":\"Marketplace store\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"));
    Long sellerId = sellers.findByUserId(applicant.getId()).orElseThrow().getId();

    mockMvc
        .perform(
            get("/api/admin/sellers?status=PENDING").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/admin/sellers?status=PENDING")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].id", hasItem(sellerId.intValue())));
    mockMvc
        .perform(
            put("/api/admin/sellers/{sellerId}/approve", sellerId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    assertThat(users.findById(applicant.getId()).orElseThrow().getRole())
        .isEqualTo(UserRole.SELLER);
  }

  @Test
  void shouldResubmitRejectedApplicationUsingSameSellerRow() throws Exception {
    User applicant = saveUser("reapply", UserRole.USER);
    User admin = saveUser("reviewer", UserRole.ADMIN);
    String userToken = tokenService.createAccessToken(applicant);
    String adminToken = tokenService.createAccessToken(admin);

    mockMvc
        .perform(
            post("/api/sellers/apply")
                .header("Authorization", "Bearer " + userToken)
                .contentType(APPLICATION_JSON)
                .content("{\"storeName\":\"First Store\"}"))
        .andExpect(status().isCreated());
    Long originalId = sellers.findByUserId(applicant.getId()).orElseThrow().getId();
    mockMvc
        .perform(
            put("/api/admin/sellers/{sellerId}/reject", originalId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/sellers/apply")
                .header("Authorization", "Bearer " + userToken)
                .contentType(APPLICATION_JSON)
                .content("{\"storeName\":\"Second Store\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(originalId))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  private User saveUser(String username, UserRole role) {
    return users.saveAndFlush(
        User.builder()
            .username(username)
            .email(username + "@example.com")
            .password("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")
            .role(role)
            .emailVerified(true)
            .createdAt(Instant.now())
            .build());
  }
}
