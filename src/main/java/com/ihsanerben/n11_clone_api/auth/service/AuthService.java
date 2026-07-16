package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.dto.LoginRequest;
import com.ihsanerben.n11_clone_api.auth.dto.RegisterRequest;
import com.ihsanerben.n11_clone_api.auth.entity.EmailVerificationToken;
import com.ihsanerben.n11_clone_api.auth.entity.RefreshToken;
import com.ihsanerben.n11_clone_api.auth.repository.EmailVerificationTokenRepository;
import com.ihsanerben.n11_clone_api.auth.repository.RefreshTokenRepository;
import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.entity.UserRole;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final EmailVerificationTokenRepository verificationTokens;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final ResendEmailService emailService;
  private final AuthProperties properties;

  @Transactional
  public void register(RegisterRequest request) {
    String username = request.username().trim();
    String email = request.email().trim().toLowerCase();

    if (users.existsByUsernameIgnoreCase(username)) {
      throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
    }
    if (users.existsByEmailIgnoreCase(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
    }

    User user =
        users.save(
            User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .emailVerified(false)
                .createdAt(Instant.now())
                .build());

    issueVerification(user);
  }

  @Transactional
  public Session login(LoginRequest request) {
    User user =
        users
            .findByUsernameIgnoreCase(request.usernameOrEmail())
            .or(() -> users.findByEmailIgnoreCase(request.usernameOrEmail()))
            .orElseThrow(() -> unauthorized("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw unauthorized("Invalid credentials");
    }
    if (!user.isEmailVerified()) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Email address is not verified");
    }

    return createSession(user);
  }

  @Transactional
  public Session refresh(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw unauthorized("Invalid refresh token");
    }

    RefreshToken current =
        refreshTokens
            .findByTokenHash(tokenService.hash(rawToken))
            .orElseThrow(() -> unauthorized("Invalid refresh token"));

    if (current.isRevoked() || !current.getExpiresAt().isAfter(Instant.now())) {
      throw unauthorized("Invalid refresh token");
    }

    current.setRevoked(true);
    return createSession(current.getUser());
  }

  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null) {
      return;
    }

    refreshTokens
        .findByTokenHash(tokenService.hash(rawToken))
        .ifPresent(token -> token.setRevoked(true));
  }

  @Transactional
  public void verifyEmail(String rawToken) {
    EmailVerificationToken token =
        verificationTokens
            .findByTokenHash(tokenService.hash(rawToken))
            .orElseThrow(
                () -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

    if (token.isUsed() || !token.getExpiresAt().isAfter(Instant.now())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification token");
    }

    token.setUsed(true);
    token.getUser().setEmailVerified(true);
  }

  @Transactional
  public void resendVerification(String email) {
    User user =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

    if (user.isEmailVerified()) {
      throw new ApiException(HttpStatus.CONFLICT, "Email address is already verified");
    }

    verificationTokens.invalidateAllForUser(user.getId());
    issueVerification(user);
  }

  private void issueVerification(User user) {
    String raw = tokenService.randomToken();
    verificationTokens.save(
        EmailVerificationToken.builder()
            .user(user)
            .tokenHash(tokenService.hash(raw))
            .expiresAt(
                Instant.now().plus(Duration.ofHours(properties.verificationExpirationHours())))
            .used(false)
            .createdAt(Instant.now())
            .build());

    emailService.sendVerification(user.getEmail(), raw);
  }

  private Session createSession(User user) {
    String raw = tokenService.randomToken();

    refreshTokens.save(
        RefreshToken.builder()
            .user(user)
            .tokenHash(tokenService.hash(raw))
            .expiresAt(Instant.now().plus(Duration.ofDays(properties.refreshExpirationDays())))
            .revoked(false)
            .createdAt(Instant.now())
            .build());

    return new Session(tokenService.createAccessToken(user), raw);
  }

  private ApiException unauthorized(String message) {
    return new ApiException(HttpStatus.UNAUTHORIZED, message);
  }

  public record Session(String accessToken, String refreshToken) {}
}
