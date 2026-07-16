package com.ihsanerben.n11_clone_api.order.repository;

import com.ihsanerben.n11_clone_api.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findAllByOrderIdOrderById(Long orderId);
}
