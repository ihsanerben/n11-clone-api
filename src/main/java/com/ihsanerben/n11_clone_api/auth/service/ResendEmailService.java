package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.EmailProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ResendEmailService {
  private final EmailProperties properties;

  public void sendVerification(String recipient, String rawToken) {
    String link = properties.frontendBaseUrl() + "/verify-email?token=" + rawToken;
    send(
        recipient,
        "Email adresinizi doğrulayın",
        "<p>Email adresinizi doğrulamak için <a href=\"" + link + "\">buraya tıklayın</a>.</p>");
  }

  public void sendPasswordReset(String recipient, String rawToken) {
    String link = properties.frontendBaseUrl() + "/reset-password?token=" + rawToken;
    send(
        recipient,
        "Şifrenizi sıfırlayın",
        "<p>Şifrenizi sıfırlamak için <a href=\"" + link + "\">buraya tıklayın</a>.</p>");
  }

  private void send(String recipient, String subject, String html) {
    RestClient.builder()
        .baseUrl("https://api.resend.com")
        .build()
        .post()
        .uri("/emails")
        .header("Authorization", "Bearer " + properties.apiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("from", properties.from(), "to", recipient, "subject", subject, "html", html))
        .retrieve()
        .toBodilessEntity();
  }
}
