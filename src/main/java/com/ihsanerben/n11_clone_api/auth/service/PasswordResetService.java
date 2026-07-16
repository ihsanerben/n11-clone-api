package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.dto.*;
import com.ihsanerben.n11_clone_api.auth.entity.PasswordResetToken;
import com.ihsanerben.n11_clone_api.auth.repository.*;
import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
  private final UserRepository users;
  private final PasswordResetTokenRepository resetTokens;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final ResendEmailService emailService;
  private final AuthProperties properties;

  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {
    users.findByEmailIgnoreCase(request.email().trim()).ifPresent(this::issueResetToken);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token =
        resetTokens
            .findByTokenHash(tokenService.hash(request.token()))
            .orElseThrow(this::invalidToken);

    if (token.isUsed() || !token.getExpiresAt().isAfter(Instant.now())) {
      throw invalidToken();
    }

    token.setUsed(true);
    token.getUser().setPassword(passwordEncoder.encode(request.newPassword()));
    refreshTokens.revokeAllForUser(token.getUser().getId());
  }

  private void issueResetToken(User user) {
    resetTokens.invalidateAllForUser(user.getId());
    String rawToken = tokenService.randomToken();
    resetTokens.save(
        PasswordResetToken.builder()
            .user(user)
            .tokenHash(tokenService.hash(rawToken))
            .expiresAt(
                Instant.now().plus(Duration.ofMinutes(properties.passwordResetExpirationMinutes())))
            .used(false)
            .createdAt(Instant.now())
            .build());
    emailService.sendPasswordReset(user.getEmail(), rawToken);
  }

  private ApiException invalidToken() {
    return new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token");
  }
}
