package com.ihsanerben.n11_clone_api.order.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CheckoutException extends ApiException {
  public CheckoutException(HttpStatus status, String message) {
    super(status, message);
  }
}
