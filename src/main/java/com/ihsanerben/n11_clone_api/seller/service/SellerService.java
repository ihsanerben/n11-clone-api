package com.ihsanerben.n11_clone_api.seller.service;

import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.seller.dto.SellerApplicationRequest;
import com.ihsanerben.n11_clone_api.seller.dto.SellerResponse;
import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import com.ihsanerben.n11_clone_api.seller.exception.SellerApplicationException;
import com.ihsanerben.n11_clone_api.seller.exception.SellerNotFoundException;
import com.ihsanerben.n11_clone_api.seller.mapper.SellerMapper;
import com.ihsanerben.n11_clone_api.seller.repository.SellerRepository;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.entity.UserRole;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerService {
  private final SellerRepository sellers;
  private final UserRepository users;
  private final SellerMapper mapper;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional
  public SellerResponse apply(SellerApplicationRequest request) {
    Long userId = authenticatedUser.userId();
    User user = users.findById(userId).orElseThrow(SellerNotFoundException::new);
    Seller seller =
        sellers
            .findByUserId(userId)
            .map(existing -> reapply(existing, request))
            .orElseGet(() -> create(user, request));
    return mapper.toResponse(sellers.save(seller));
  }

  @Transactional(readOnly = true)
  public SellerResponse getMyApplication() {
    Long userId = authenticatedUser.userId();
    return sellers
        .findByUserId(userId)
        .map(mapper::toResponse)
        .orElseThrow(SellerNotFoundException::new);
  }

  @Transactional(readOnly = true)
  public Page<SellerResponse> list(SellerStatus status, Pageable pageable) {
    Page<Seller> result =
        status == null ? sellers.findAll(pageable) : sellers.findAllByStatus(status, pageable);
    return result.map(mapper::toResponse);
  }

  @Transactional
  public SellerResponse approve(Long sellerId) {
    Seller seller = pendingSeller(sellerId);
    seller.setStatus(SellerStatus.APPROVED);
    seller.setReviewedAt(Instant.now());
    seller.getUser().setRole(UserRole.SELLER);
    return mapper.toResponse(seller);
  }

  @Transactional
  public SellerResponse reject(Long sellerId) {
    Seller seller = pendingSeller(sellerId);
    seller.setStatus(SellerStatus.REJECTED);
    seller.setReviewedAt(Instant.now());
    return mapper.toResponse(seller);
  }

  private Seller create(User user, SellerApplicationRequest request) {
    return Seller.builder()
        .user(user)
        .storeName(request.storeName().trim())
        .description(normalize(request.description()))
        .status(SellerStatus.PENDING)
        .appliedAt(Instant.now())
        .build();
  }

  private Seller reapply(Seller seller, SellerApplicationRequest request) {
    if (seller.getStatus() != SellerStatus.REJECTED) {
      throw new SellerApplicationException("Only a rejected application can be submitted again");
    }
    seller.setStoreName(request.storeName().trim());
    seller.setDescription(normalize(request.description()));
    seller.setStatus(SellerStatus.PENDING);
    seller.setAppliedAt(Instant.now());
    seller.setReviewedAt(null);
    return seller;
  }

  private Seller pendingSeller(Long sellerId) {
    Seller seller = sellers.findById(sellerId).orElseThrow(SellerNotFoundException::new);
    if (seller.getStatus() != SellerStatus.PENDING) {
      throw new SellerApplicationException("Only a pending application can be reviewed");
    }
    return seller;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
