package com.ihsanerben.n11_clone_api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.user.entity.*;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {
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
    registry.add("app.auth.refresh-cookie-name", () -> "refreshToken");
    registry.add("app.auth.password-reset-expiration-minutes", () -> "60");
    registry.add("app.auth.cookie-secure", () -> "false");
    registry.add("app.email.api-key", () -> "test-key");
    registry.add("app.email.from", () -> "test@example.com");
    registry.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @MockitoBean ResendEmailService emailService;

  @Test
  void shouldCompleteAuthLifecycleAndRotateRefreshToken() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"username\":\"ihsan\",\"email\":\"ihsan@example.com\",\"password\":\"StrongPass1\"}"))
        .andExpect(status().isCreated());

    var tokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(emailService).sendVerification(eq("ihsan@example.com"), tokenCaptor.capture());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"ihsan\",\"password\":\"StrongPass1\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType(APPLICATION_JSON)
                .content("{\"token\":\"" + tokenCaptor.getValue() + "\"}"))
        .andExpect(status().isOk());

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content("{\"usernameOrEmail\":\"ihsan\",\"password\":\"StrongPass1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(
                header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
            .andReturn();
    Cookie first = login.getResponse().getCookie("refreshToken");
    assertThat(first).isNotNull();

    MvcResult refreshed =
        mockMvc
            .perform(post("/api/auth/refresh").cookie(first))
            .andExpect(status().isOk())
            .andReturn();
    Cookie second = refreshed.getResponse().getCookie("refreshToken");
    assertThat(second).isNotNull();
    assertThat(second.getValue()).isNotEqualTo(first.getValue());
    mockMvc.perform(post("/api/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/auth/logout").cookie(second))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    mockMvc.perform(post("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInvalidRegistration() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"x\",\"email\":\"invalid\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.email").exists());
  }

  @Test
  void shouldResetPasswordWithSingleUseToken() throws Exception {
    users.save(
        User.builder()
            .username("reset-user")
            .email("reset@example.com")
            .password(passwordEncoder.encode("OldPassword1"))
            .role(UserRole.USER)
            .emailVerified(true)
            .createdAt(java.time.Instant.now())
            .build());
    clearInvocations(emailService);

    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
        .andExpect(status().isOk());
    var tokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(emailService).sendPasswordReset(eq("reset@example.com"), tokenCaptor.capture());
    String resetBody =
        "{\"token\":\"" + tokenCaptor.getValue() + "\",\"newPassword\":\"NewPassword1\"}";

    mockMvc
        .perform(post("/api/auth/reset-password").contentType(APPLICATION_JSON).content(resetBody))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/auth/reset-password").contentType(APPLICATION_JSON).content(resetBody))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"reset-user\",\"password\":\"NewPassword1\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldNotRevealWhetherForgotPasswordEmailExists() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value("If the email is registered, a password reset link has been sent"));
  }
}
