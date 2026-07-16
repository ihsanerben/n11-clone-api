package com.ihsanerben.n11_clone_api.order.controller;

import com.ihsanerben.n11_clone_api.order.dto.OrderResponse;
import com.ihsanerben.n11_clone_api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class BuyerOrderController {
  private final OrderService service;

  @GetMapping
  public Page<OrderResponse> list(@ParameterObject Pageable pageable) {
    return service.listBuyerOrders(pageable);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable Long id) {
    return service.getBuyerOrder(id);
  }

  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(@PathVariable Long id) {
    return service.cancelBuyerOrder(id);
  }
}
