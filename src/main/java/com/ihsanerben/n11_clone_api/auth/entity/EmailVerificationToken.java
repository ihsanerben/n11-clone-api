package com.ihsanerben.n11_clone_api.auth.entity;

import com.ihsanerben.n11_clone_api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "email_verification_tokens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailVerificationToken {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
	@Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
	@Column(name = "expires_at", nullable = false) private Instant expiresAt;
	@Column(nullable = false) private boolean used;
	@Column(name = "created_at", nullable = false) private Instant createdAt;
}
