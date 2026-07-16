package com.ihsanerben.n11_clone_api.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    int stockQuantity,
    String imageUrl,
    boolean active,
    int version,
    Long categoryId,
    String categoryName,
    Long sellerId,
    String storeName,
    Instant createdAt,
    Instant updatedAt) {}
