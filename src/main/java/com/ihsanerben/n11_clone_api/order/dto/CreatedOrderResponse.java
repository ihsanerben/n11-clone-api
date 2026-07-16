package com.ihsanerben.n11_clone_api.order.dto;

import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record CreatedOrderResponse(
    Long orderId,
    Long sellerId,
    String storeName,
    OrderStatus status,
    BigDecimal totalAmount,
    List<CreatedOrderItemResponse> items) {}
