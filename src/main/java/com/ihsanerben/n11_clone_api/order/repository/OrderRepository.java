package com.ihsanerben.n11_clone_api.order.repository;

import com.ihsanerben.n11_clone_api.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}
