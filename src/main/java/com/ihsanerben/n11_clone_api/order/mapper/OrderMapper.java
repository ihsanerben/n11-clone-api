package com.ihsanerben.n11_clone_api.order.mapper;

import com.ihsanerben.n11_clone_api.order.dto.OrderItemResponse;
import com.ihsanerben.n11_clone_api.order.dto.OrderResponse;
import com.ihsanerben.n11_clone_api.order.entity.Order;
import com.ihsanerben.n11_clone_api.order.entity.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
  public OrderResponse toResponse(Order order, List<OrderItem> items) {
    return new OrderResponse(
        order.getId(),
        order.getBuyer().getId(),
        order.getBuyer().getUsername(),
        order.getSeller().getId(),
        order.getSeller().getStoreName(),
        order.getStatus(),
        order.getTotalAmount(),
        order.getRecipientName(),
        order.getPhone(),
        order.getAddressLine(),
        order.getCity(),
        order.getPostalCode(),
        order.getCreatedAt(),
        items.stream().map(this::toItemResponse).toList());
  }

  private OrderItemResponse toItemResponse(OrderItem item) {
    BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    return new OrderItemResponse(
        item.getId(),
        item.getProduct().getId(),
        item.getProduct().getName(),
        item.getQuantity(),
        item.getUnitPrice(),
        lineTotal);
  }
}
