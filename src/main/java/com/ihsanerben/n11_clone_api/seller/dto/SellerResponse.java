package com.ihsanerben.n11_clone_api.seller.dto;

import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import java.time.Instant;

public record SellerResponse(
    Long id,
    Long userId,
    String username,
    String email,
    String storeName,
    String description,
    SellerStatus status,
    Instant appliedAt,
    Instant reviewedAt) {}
