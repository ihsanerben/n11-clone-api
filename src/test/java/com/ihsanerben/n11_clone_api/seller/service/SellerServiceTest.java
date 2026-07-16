package com.ihsanerben.n11_clone_api.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import com.ihsanerben.n11_clone_api.seller.mapper.SellerMapper;
import com.ihsanerben.n11_clone_api.seller.repository.SellerRepository;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.entity.UserRole;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {
  @Mock SellerRepository sellers;
  @Mock UserRepository users;
  @Mock AuthenticatedUserProvider authenticatedUser;

  @Test
  void shouldApprovePendingSellerAndPromoteUserInOneOperation() {
    User user =
        User.builder()
            .id(7L)
            .username("seller")
            .email("seller@example.com")
            .role(UserRole.USER)
            .build();
    Seller seller =
        Seller.builder()
            .id(11L)
            .user(user)
            .storeName("Store")
            .status(SellerStatus.PENDING)
            .appliedAt(Instant.now())
            .build();
    when(sellers.findById(11L)).thenReturn(Optional.of(seller));
    SellerService service =
        new SellerService(sellers, users, new SellerMapper(), authenticatedUser);

    var response = service.approve(11L);

    assertThat(response.status()).isEqualTo(SellerStatus.APPROVED);
    assertThat(response.reviewedAt()).isNotNull();
    assertThat(user.getRole()).isEqualTo(UserRole.SELLER);
  }
}
