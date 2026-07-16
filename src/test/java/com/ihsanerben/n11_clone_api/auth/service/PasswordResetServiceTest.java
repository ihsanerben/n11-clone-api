package com.ihsanerben.n11_clone_api.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.dto.*;
import com.ihsanerben.n11_clone_api.auth.repository.*;
import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
  @Mock UserRepository users;
  @Mock PasswordResetTokenRepository resetTokens;
  @Mock RefreshTokenRepository refreshTokens;
  @Mock PasswordEncoder passwordEncoder;
  @Mock TokenService tokenService;
  @Mock ResendEmailService emailService;
  PasswordResetService service;

  @BeforeEach
  void setUp() {
    AuthProperties properties =
        new AuthProperties(
            "unit-test-secret-key-at-least-32-characters",
            1_200_000,
            14,
            24,
            60,
            "refreshToken",
            false);
    service =
        new PasswordResetService(
            users,
            resetTokens,
            refreshTokens,
            passwordEncoder,
            tokenService,
            emailService,
            properties);
  }

  @Test
  void shouldSilentlyIgnoreUnknownEmail() {
    when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    service.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

    verifyNoInteractions(resetTokens, emailService);
  }

  @Test
  void shouldRejectUnknownResetToken() {
    when(tokenService.hash("unknown-token")).thenReturn("hash");
    when(resetTokens.findByTokenHash("hash")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.resetPassword(new ResetPasswordRequest("unknown-token", "NewPassword1")))
        .isInstanceOf(ApiException.class)
        .hasMessage("Invalid or expired password reset token");
  }
}
