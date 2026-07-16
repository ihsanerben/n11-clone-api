package com.ihsanerben.n11_clone_api.order.dto;

import java.math.BigDecimal;

public record CreatedOrderItemResponse(
    Long productId, String productName, int quantity, BigDecimal unitPrice) {}
