package com.ihsanerben.n11_clone_api.auth.repository;

import com.ihsanerben.n11_clone_api.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
	void invalidateAllForUser(Long userId);
}
