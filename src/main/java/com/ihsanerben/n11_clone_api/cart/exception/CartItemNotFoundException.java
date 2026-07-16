package com.ihsanerben.n11_clone_api.cart.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends ApiException {
  public CartItemNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Cart item not found");
  }
}
