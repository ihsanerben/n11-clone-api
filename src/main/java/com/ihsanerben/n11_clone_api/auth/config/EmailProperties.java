package com.ihsanerben.n11_clone_api.auth.config;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Validated
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
		@NotBlank String apiKey,
		@NotBlank @Email String from,
		@NotBlank String frontendBaseUrl
) {
}
