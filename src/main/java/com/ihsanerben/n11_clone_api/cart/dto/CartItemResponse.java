package com.ihsanerben.n11_clone_api.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
    Long id,
    Long productId,
    String productName,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal,
    int availableStock,
    Long sellerId,
    String storeName) {}
