package com.ihsanerben.n11_clone_api.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.service.AuthService;
import com.ihsanerben.n11_clone_api.auth.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthCookiePolicyTest {
  @Mock AuthService authService;
  @Mock PasswordResetService passwordResetService;

  MockMvc mvc;

  @BeforeEach
  void setUp() {
    AuthProperties properties =
        new AuthProperties(
            "production-test-secret-key-at-least-32-characters",
            1_200_000,
            14,
            24,
            60,
            "refreshToken",
            true);
    AuthController controller = new AuthController(authService, passwordResetService, properties);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void shouldUseCrossSiteSecureCookiePolicyInProduction() throws Exception {
    when(authService.login(any()))
        .thenReturn(new AuthService.Session("access-token", "refresh-token"));

    mvc.perform(
            post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"user\",\"password\":\"StrongPass1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", containsString("Secure")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=None")))
        .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=1209600")));
  }

  @Test
  void shouldClearCookieWithTheProductionPolicy() throws Exception {
    Cookie refreshCookie = new Cookie("refreshToken", "refresh-token");

    mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
        .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=None")))
        .andExpect(header().string("Set-Cookie", containsString("Secure")));
  }
}
