package com.ihsanerben.n11_clone_api.order.service;

import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.order.dto.OrderResponse;
import com.ihsanerben.n11_clone_api.order.entity.Order;
import com.ihsanerben.n11_clone_api.order.entity.OrderItem;
import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import com.ihsanerben.n11_clone_api.order.exception.InvalidOrderStatusTransitionException;
import com.ihsanerben.n11_clone_api.order.exception.OrderNotFoundException;
import com.ihsanerben.n11_clone_api.order.mapper.OrderMapper;
import com.ihsanerben.n11_clone_api.order.repository.OrderItemRepository;
import com.ihsanerben.n11_clone_api.order.repository.OrderRepository;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orders;
  private final OrderItemRepository orderItems;
  private final ProductRepository products;
  private final OrderMapper mapper;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional(readOnly = true)
  public Page<OrderResponse> listBuyerOrders(Pageable pageable) {
    return mapPage(orders.findAllByBuyerId(userId(), pageable));
  }

  @Transactional(readOnly = true)
  public OrderResponse getBuyerOrder(Long id) {
    return response(
        orders.findByIdAndBuyerId(id, userId()).orElseThrow(OrderNotFoundException::new));
  }

  @Transactional
  public OrderResponse cancelBuyerOrder(Long id) {
    Order order = orders.findByIdAndBuyerId(id, userId()).orElseThrow(OrderNotFoundException::new);
    if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
      throw transition(order.getStatus(), OrderStatus.CANCELLED);
    }
    List<OrderItem> items = items(order.getId());
    restoreStock(items);
    order.changeStatus(OrderStatus.CANCELLED);
    products.flush();
    return mapper.toResponse(order, items);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> listSellerOrders(Pageable pageable) {
    return mapPage(orders.findAllBySellerUserId(userId(), pageable));
  }

  @Transactional
  public OrderResponse updateSellerStatus(Long id, OrderStatus requestedStatus) {
    Order order =
        orders.findByIdAndSellerUserId(id, userId()).orElseThrow(OrderNotFoundException::new);
    OrderStatus expected = nextSellerStatus(order.getStatus());
    if (requestedStatus != expected) {
      throw transition(order.getStatus(), requestedStatus);
    }
    order.changeStatus(requestedStatus);
    return response(order);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> listAdminOrders(Pageable pageable) {
    return mapPage(orders.findAll(pageable));
  }

  @Transactional
  public OrderResponse updateAdminStatus(Long id, OrderStatus requestedStatus) {
    Order order = orders.findById(id).orElseThrow(OrderNotFoundException::new);
    List<OrderItem> items = items(order.getId());
    if (order.getStatus() != OrderStatus.CANCELLED && requestedStatus == OrderStatus.CANCELLED) {
      restoreStock(items);
      products.flush();
    } else if (order.getStatus() == OrderStatus.CANCELLED
        && requestedStatus != OrderStatus.CANCELLED) {
      validateStock(items);
      decreaseStock(items);
      products.flush();
    }
    order.changeStatus(requestedStatus);
    return mapper.toResponse(order, items);
  }

  private Page<OrderResponse> mapPage(Page<Order> page) {
    List<Long> orderIds = page.getContent().stream().map(Order::getId).toList();
    Map<Long, List<OrderItem>> itemsByOrder =
        orderIds.isEmpty()
            ? Collections.emptyMap()
            : orderItems.findAllByOrderIdInOrderById(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
    return page.map(
        order -> mapper.toResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of())));
  }

  private OrderResponse response(Order order) {
    return mapper.toResponse(order, items(order.getId()));
  }

  private List<OrderItem> items(Long orderId) {
    return orderItems.findAllByOrderIdOrderById(orderId);
  }

  private void restoreStock(List<OrderItem> items) {
    for (OrderItem item : items) {
      Product product = item.getProduct();
      product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
    }
  }

  private void validateStock(List<OrderItem> items) {
    for (OrderItem item : items) {
      if (item.getQuantity() > item.getProduct().getStockQuantity()) {
        throw new InvalidOrderStatusTransitionException(
            "Insufficient stock to reactivate cancelled order");
      }
    }
  }

  private void decreaseStock(List<OrderItem> items) {
    for (OrderItem item : items) {
      Product product = item.getProduct();
      product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
    }
  }

  private OrderStatus nextSellerStatus(OrderStatus currentStatus) {
    return switch (currentStatus) {
      case PENDING -> OrderStatus.CONFIRMED;
      case CONFIRMED -> OrderStatus.SHIPPED;
      case SHIPPED -> OrderStatus.DELIVERED;
      case DELIVERED, CANCELLED -> null;
    };
  }

  private InvalidOrderStatusTransitionException transition(
      OrderStatus currentStatus, OrderStatus requestedStatus) {
    return new InvalidOrderStatusTransitionException(
        "Order status cannot change from " + currentStatus + " to " + requestedStatus);
  }

  private Long userId() {
    return authenticatedUser.userId();
  }
}
