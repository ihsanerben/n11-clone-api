package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service @RequiredArgsConstructor
public class ResendEmailService {
	private final EmailProperties properties;
	public void sendVerification(String recipient, String rawToken) {
		String link = properties.frontendBaseUrl() + "/verify-email?token=" + rawToken;
		RestClient.builder().baseUrl("https://api.resend.com").build().post().uri("/emails")
				.header("Authorization", "Bearer " + properties.apiKey()).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("from", properties.from(), "to", recipient, "subject", "Email adresinizi doğrulayın",
						"html", "<p>Email adresinizi doğrulamak için <a href=\"" + link + "\">buraya tıklayın</a>.</p>"))
				.retrieve().toBodilessEntity();
	}
}
