package com.ihsanerben.n11_clone_api.address.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.service.ResendEmailService;
import com.ihsanerben.n11_clone_api.auth.service.TokenService;
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
class AddressControllerIT {
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
    registry.add("app.email.api-key", () -> "test");
    registry.add("app.email.from", () -> "test@example.com");
    registry.add("app.email.frontend-base-url", () -> "http://localhost:5173");
  }

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired TokenService tokens;
  @MockitoBean ResendEmailService email;

  @Test
  void shouldManageOnlyOwnAddressesAndKeepOneDefault() throws Exception {
    String ownerToken = tokenFor("address-owner");
    String otherToken = tokenFor("address-other");

    long homeId = create(ownerToken, "Home", false);
    mvc.perform(get("/api/addresses").header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(homeId))
        .andExpect(jsonPath("$[0].defaultAddress").value(true));

    long workId = create(ownerToken, "Work", true);
    mvc.perform(get("/api/addresses").header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(workId))
        .andExpect(jsonPath("$[0].defaultAddress").value(true))
        .andExpect(jsonPath("$[1].defaultAddress").value(false));

    mvc.perform(get("/api/addresses/{id}", workId).header("Authorization", bearer(otherToken)))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/addresses/{id}", homeId)
                .header("Authorization", bearer(ownerToken))
                .contentType(APPLICATION_JSON)
                .content(body("Home updated", true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultAddress").value(true));
    mvc.perform(delete("/api/addresses/{id}", homeId).header("Authorization", bearer(ownerToken)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/addresses").header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(workId))
        .andExpect(jsonPath("$[0].defaultAddress").value(true));
  }

  private long create(String token, String label, boolean defaultAddress) throws Exception {
    String response =
        mvc.perform(
                post("/api/addresses")
                    .header("Authorization", bearer(token))
                    .contentType(APPLICATION_JSON)
                    .content(body(label, defaultAddress)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return Long.parseLong(response.replaceAll(".*\\\"id\\\":([0-9]+).*", "$1"));
  }

  private String tokenFor(String username) {
    User user =
        users.saveAndFlush(
            User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")
                .role(UserRole.USER)
                .emailVerified(true)
                .createdAt(Instant.now())
                .build());
    return tokens.createAccessToken(user);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String body(String label, boolean defaultAddress) {
    return """
        {
          "label": "%s",
          "recipientName": "Test User",
          "phone": "+90 555 111 22 33",
          "addressLine": "Example Street 10",
          "city": "Istanbul",
          "postalCode": "34000",
          "defaultAddress": %s
        }
        """
        .formatted(label, defaultAddress);
  }
}
