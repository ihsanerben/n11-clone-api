package com.ihsanerben.n11_clone_api.cart.mapper;

import com.ihsanerben.n11_clone_api.cart.dto.CartItemResponse;
import com.ihsanerben.n11_clone_api.cart.dto.CartResponse;
import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {
  public CartResponse toResponse(Long cartId, List<CartItem> cartItems) {
    List<CartItemResponse> items = cartItems.stream().map(this::toItemResponse).toList();
    BigDecimal total =
        items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartResponse(cartId, items, total);
  }

  private CartItemResponse toItemResponse(CartItem item) {
    var product = item.getProduct();
    BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemResponse(
        item.getId(),
        product.getId(),
        product.getName(),
        product.getImageUrl(),
        product.getPrice(),
        item.getQuantity(),
        lineTotal,
        product.getStockQuantity(),
        product.getSeller().getId(),
        product.getSeller().getStoreName());
  }
}
