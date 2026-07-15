package com.ihsanerben.n11_clone_api.auth.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		@NotBlank @Size(min = 32) String jwtSecret,
		@Positive long accessExpirationMs,
		@Positive long refreshExpirationDays,
		@Positive long verificationExpirationHours,
		@NotBlank String refreshCookieName,
		boolean cookieSecure
) {}
