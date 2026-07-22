package com.ihsanerben.n11_clone_api.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
    @NotBlank @Email String from, @NotBlank String frontendBaseUrl) {}
