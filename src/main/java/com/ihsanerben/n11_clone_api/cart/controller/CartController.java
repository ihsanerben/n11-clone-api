package com.ihsanerben.n11_clone_api.cart.controller;

import com.ihsanerben.n11_clone_api.cart.dto.AddCartItemRequest;
import com.ihsanerben.n11_clone_api.cart.dto.CartResponse;
import com.ihsanerben.n11_clone_api.cart.dto.UpdateCartItemRequest;
import com.ihsanerben.n11_clone_api.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
  private final CartService service;

  @GetMapping
  public CartResponse get() {
    return service.get();
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  public CartResponse add(@Valid @RequestBody AddCartItemRequest request) {
    return service.add(request);
  }

  @PutMapping("/items/{itemId}")
  public CartResponse update(
      @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
    return service.update(itemId, request);
  }

  @DeleteMapping("/items/{itemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeItem(@PathVariable Long itemId) {
    service.removeItem(itemId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear() {
    service.clear();
  }
}
