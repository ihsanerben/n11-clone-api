package com.ihsanerben.n11_clone_api.auth.repository;

import com.ihsanerben.n11_clone_api.auth.entity.EmailVerificationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {
  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update EmailVerificationToken t set t.used = true where t.user.id = :userId and t.used = false")
  void invalidateAllForUser(Long userId);
}
