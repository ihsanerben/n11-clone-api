package com.ihsanerben.n11_clone_api.cart.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CartStockException extends ApiException {
  public CartStockException(String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
