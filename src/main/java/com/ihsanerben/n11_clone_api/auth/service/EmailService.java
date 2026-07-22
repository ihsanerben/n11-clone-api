package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender mailSender;
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
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(properties.from());
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
    } catch (MessagingException | MailException exception) {
      throw new EmailDeliveryException(exception);
    }
  }
}
