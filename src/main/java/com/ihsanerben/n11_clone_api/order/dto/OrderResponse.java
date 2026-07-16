package com.ihsanerben.n11_clone_api.order.dto;

import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Long buyerId,
    String buyerUsername,
    Long sellerId,
    String storeName,
    OrderStatus status,
    BigDecimal totalAmount,
    String recipientName,
    String phone,
    String addressLine,
    String city,
    String postalCode,
    Instant createdAt,
    List<OrderItemResponse> items) {}
