package com.ihsanerben.n11_clone_api.order.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidOrderStatusTransitionException extends ApiException {
  public InvalidOrderStatusTransitionException(String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
