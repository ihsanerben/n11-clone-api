package com.ihsanerben.n11_clone_api.order.controller;

import com.ihsanerben.n11_clone_api.order.dto.CheckoutRequest;
import com.ihsanerben.n11_clone_api.order.dto.CheckoutResponse;
import com.ihsanerben.n11_clone_api.order.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CheckoutController {
  private final CheckoutService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
    return service.checkout(request.addressId());
  }
}
