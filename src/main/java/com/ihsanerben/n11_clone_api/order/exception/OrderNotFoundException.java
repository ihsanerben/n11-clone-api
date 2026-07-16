package com.ihsanerben.n11_clone_api.order.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {
  public OrderNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Order not found");
  }
}
