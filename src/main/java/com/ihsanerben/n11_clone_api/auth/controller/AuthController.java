package com.ihsanerben.n11_clone_api.auth.controller;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.dto.*;
import com.ihsanerben.n11_clone_api.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor @Tag(name = "Authentication")
public class AuthController {
	private final AuthService service;
	private final AuthProperties properties;

	@PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a user and send an email verification link")
	public MessageResponse register(@Valid @RequestBody RegisterRequest request) { service.register(request); return new MessageResponse("Registration successful. Check your email."); }
	@PostMapping("/login")
	@Operation(summary = "Log in and create an access/refresh token session")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) { return session(service.login(request)); }
	@PostMapping("/refresh")
	@Operation(summary = "Rotate the refresh token and issue a new access token")
	public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) { return session(service.refresh(cookie(request))); }
	@PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Revoke the refresh token and clear its cookie")
	public ResponseEntity<Void> logout(HttpServletRequest request) { service.logout(cookie(request)); return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build(); }
	@PostMapping("/verify-email")
	@Operation(summary = "Verify an email address")
	public MessageResponse verify(@Valid @RequestBody VerifyEmailRequest request) { service.verifyEmail(request.token()); return new MessageResponse("Email verified"); }
	@PostMapping("/resend-verification")
	@Operation(summary = "Replace and resend an email verification token")
	public MessageResponse resend(@Valid @RequestBody ResendVerificationRequest request) { service.resendVerification(request.email()); return new MessageResponse("Verification email sent"); }

	private ResponseEntity<TokenResponse> session(AuthService.Session session) {
		ResponseCookie cookie = ResponseCookie.from(properties.refreshCookieName(), session.refreshToken()).httpOnly(true)
				.secure(properties.cookieSecure()).sameSite("None").path("/api/auth").maxAge(Duration.ofDays(properties.refreshExpirationDays())).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(new TokenResponse(session.accessToken(), "Bearer", properties.accessExpirationMs() / 1000));
	}
	private ResponseCookie clearCookie() { return ResponseCookie.from(properties.refreshCookieName(), "").httpOnly(true).secure(properties.cookieSecure()).sameSite("None").path("/api/auth").maxAge(0).build(); }
	private String cookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (var cookie : request.getCookies()) if (properties.refreshCookieName().equals(cookie.getName())) return cookie.getValue();
		return null;
	}
}
