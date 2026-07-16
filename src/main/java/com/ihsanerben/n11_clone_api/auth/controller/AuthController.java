package com.ihsanerben.n11_clone_api.auth.controller;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.dto.ForgotPasswordRequest;
import com.ihsanerben.n11_clone_api.auth.dto.LoginRequest;
import com.ihsanerben.n11_clone_api.auth.dto.MessageResponse;
import com.ihsanerben.n11_clone_api.auth.dto.RegisterRequest;
import com.ihsanerben.n11_clone_api.auth.dto.ResendVerificationRequest;
import com.ihsanerben.n11_clone_api.auth.dto.ResetPasswordRequest;
import com.ihsanerben.n11_clone_api.auth.dto.TokenResponse;
import com.ihsanerben.n11_clone_api.auth.dto.VerifyEmailRequest;
import com.ihsanerben.n11_clone_api.auth.service.AuthService;
import com.ihsanerben.n11_clone_api.auth.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
	private static final String REFRESH_COOKIE_PATH = "/api/auth";

	private final AuthService authService;
	private final PasswordResetService passwordResetService;
	private final AuthProperties properties;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a user and send an email verification link")
	public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request);
		return new MessageResponse("Registration successful. Check your email.");
	}

	@PostMapping("/login")
	@Operation(summary = "Log in and create an access/refresh token session")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return session(authService.login(request));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Rotate the refresh token and issue a new access token")
	public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
		return session(authService.refresh(readRefreshToken(request)));
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke the refresh token and clear its cookie")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(readRefreshToken(request));

		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, createClearedRefreshCookie().toString())
				.build();
	}

	@PostMapping("/verify-email")
	@Operation(summary = "Verify an email address")
	public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request.token());
		return new MessageResponse("Email verified");
	}

	@PostMapping("/resend-verification")
	@Operation(summary = "Replace and resend an email verification token")
	public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
		authService.resendVerification(request.email());
		return new MessageResponse("Verification email sent");
	}

	@PostMapping("/forgot-password")
	@Operation(summary = "Send a password reset email if the account exists")
	public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordResetService.forgotPassword(request);
		return new MessageResponse("If the email is registered, a password reset link has been sent");
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset a password with a valid single-use token")
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.resetPassword(request);
		return new MessageResponse("Password reset successful");
	}

	private ResponseEntity<TokenResponse> session(AuthService.Session session) {
		ResponseCookie refreshCookie = createRefreshCookie(session.refreshToken());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
				.body(new TokenResponse(session.accessToken(), "Bearer", properties.accessExpirationMs() / 1000));
	}

	private ResponseCookie createRefreshCookie(String refreshToken) {
		return refreshCookieBuilder(refreshToken)
				.maxAge(Duration.ofDays(properties.refreshExpirationDays()))
				.build();
	}

	private ResponseCookie createClearedRefreshCookie() {
		return refreshCookieBuilder("")
				.maxAge(Duration.ZERO)
				.build();
	}

	private ResponseCookie.ResponseCookieBuilder refreshCookieBuilder(String value) {
		return ResponseCookie.from(properties.refreshCookieName(), value)
				.httpOnly(true)
				.secure(properties.cookieSecure())
				.sameSite("None")
				.path(REFRESH_COOKIE_PATH);
	}

	private String readRefreshToken(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}

		for (var cookie : request.getCookies()) {
			if (properties.refreshCookieName().equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
