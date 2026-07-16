package com.ihsanerben.n11_clone_api.order.repository;

import com.ihsanerben.n11_clone_api.order.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"buyer", "seller"})
  Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);

  @EntityGraph(attributePaths = {"buyer", "seller"})
  Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

  @EntityGraph(attributePaths = {"buyer", "seller"})
  Page<Order> findAllBySellerUserId(Long sellerUserId, Pageable pageable);

  @EntityGraph(attributePaths = {"buyer", "seller"})
  Optional<Order> findByIdAndSellerUserId(Long id, Long sellerUserId);

  @Override
  @EntityGraph(attributePaths = {"buyer", "seller"})
  Page<Order> findAll(Pageable pageable);
}
