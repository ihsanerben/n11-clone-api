package com.ihsanerben.n11_clone_api.auth.repository;

import com.ihsanerben.n11_clone_api.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
  void revokeAllForUser(Long userId);
}
