package com.ihsanerben.n11_clone_api.order.controller;

import com.ihsanerben.n11_clone_api.order.dto.OrderResponse;
import com.ihsanerben.n11_clone_api.order.dto.UpdateOrderStatusRequest;
import com.ihsanerben.n11_clone_api.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
  private final OrderService service;

  @GetMapping
  public Page<OrderResponse> list(@ParameterObject Pageable pageable) {
    return service.listAdminOrders(pageable);
  }

  @PutMapping("/{id}/status")
  public OrderResponse updateStatus(
      @PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
    return service.updateAdminStatus(id, request.status());
  }
}
