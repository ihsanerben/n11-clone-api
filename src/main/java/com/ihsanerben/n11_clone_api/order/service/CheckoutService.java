package com.ihsanerben.n11_clone_api.order.service;

import com.ihsanerben.n11_clone_api.address.entity.Address;
import com.ihsanerben.n11_clone_api.address.exception.AddressNotFoundException;
import com.ihsanerben.n11_clone_api.address.repository.AddressRepository;
import com.ihsanerben.n11_clone_api.cart.entity.CartItem;
import com.ihsanerben.n11_clone_api.cart.repository.CartItemRepository;
import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.order.dto.CheckoutResponse;
import com.ihsanerben.n11_clone_api.order.dto.CreatedOrderItemResponse;
import com.ihsanerben.n11_clone_api.order.dto.CreatedOrderResponse;
import com.ihsanerben.n11_clone_api.order.entity.Order;
import com.ihsanerben.n11_clone_api.order.entity.OrderItem;
import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import com.ihsanerben.n11_clone_api.order.exception.CheckoutException;
import com.ihsanerben.n11_clone_api.order.repository.OrderItemRepository;
import com.ihsanerben.n11_clone_api.order.repository.OrderRepository;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {
  private final AddressRepository addresses;
  private final CartItemRepository cartItems;
  private final OrderRepository orders;
  private final OrderItemRepository orderItems;
  private final ProductRepository products;
  private final UserRepository users;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional
  public CheckoutResponse checkout(Long addressId) {
    Long userId = authenticatedUser.userId();
    Address address =
        addresses.findByIdAndUserId(addressId, userId).orElseThrow(AddressNotFoundException::new);
    List<CartItem> items = cartItems.findAllByCartUserIdOrderById(userId);
    if (items.isEmpty()) {
      throw new CheckoutException(HttpStatus.BAD_REQUEST, "Cart is empty");
    }

    validateAll(items);
    items.forEach(this::decreaseStock);
    products.flush();

    Map<Long, List<CartItem>> itemsBySeller = groupBySeller(items);
    List<CreatedOrderResponse> createdOrders = new ArrayList<>();
    for (List<CartItem> sellerItems : itemsBySeller.values()) {
      createdOrders.add(createOrder(userId, address, sellerItems));
    }
    cartItems.deleteAllByCartUserId(userId);
    return new CheckoutResponse(createdOrders);
  }

  private void validateAll(List<CartItem> items) {
    for (CartItem item : items) {
      Product product = item.getProduct();
      if (!product.isActive()) {
        throw new CheckoutException(
            HttpStatus.CONFLICT, "Product is no longer available: " + product.getName());
      }
      if (item.getQuantity() > product.getStockQuantity()) {
        throw new CheckoutException(
            HttpStatus.CONFLICT, "Insufficient stock for product: " + product.getName());
      }
    }
  }

  private void decreaseStock(CartItem item) {
    Product product = item.getProduct();
    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
  }

  private Map<Long, List<CartItem>> groupBySeller(List<CartItem> items) {
    Map<Long, List<CartItem>> grouped = new LinkedHashMap<>();
    for (CartItem item : items) {
      Long sellerId = item.getProduct().getSeller().getId();
      grouped.computeIfAbsent(sellerId, ignored -> new ArrayList<>()).add(item);
    }
    return grouped;
  }

  private CreatedOrderResponse createOrder(
      Long userId, Address address, List<CartItem> sellerItems) {
    Seller seller = sellerItems.getFirst().getProduct().getSeller();
    BigDecimal total =
        sellerItems.stream().map(this::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    Order order =
        orders.save(
            Order.builder()
                .buyer(users.getReferenceById(userId))
                .seller(seller)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .createdAt(Instant.now())
                .build());

    List<OrderItem> savedItems =
        orderItems.saveAll(
            sellerItems.stream()
                .map(
                    item ->
                        OrderItem.builder()
                            .order(order)
                            .product(item.getProduct())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getProduct().getPrice())
                            .build())
                .toList());
    List<CreatedOrderItemResponse> itemResponses =
        savedItems.stream()
            .map(
                item ->
                    new CreatedOrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()))
            .toList();
    return new CreatedOrderResponse(
        order.getId(),
        seller.getId(),
        seller.getStoreName(),
        order.getStatus(),
        order.getTotalAmount(),
        itemResponses);
  }

  private BigDecimal lineTotal(CartItem item) {
    return item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
  }
}
