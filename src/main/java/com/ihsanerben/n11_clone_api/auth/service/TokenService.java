package com.ihsanerben.n11_clone_api.auth.service;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {
	private static final int OPAQUE_TOKEN_BYTES = 32;

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;
	private final SecureRandom random = new SecureRandom();

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("n11-clone-api")
				.issuedAt(now)
				.expiresAt(now.plusMillis(properties.accessExpirationMs()))
				.subject(user.getUsername())
				.claim("userId", user.getId())
				.claim("roles", List.of(user.getRole().name()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String randomToken() {
		byte[] bytes = new byte[OPAQUE_TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
		}
	}
}
