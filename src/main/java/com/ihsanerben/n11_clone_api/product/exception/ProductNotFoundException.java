package com.ihsanerben.n11_clone_api.product.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {
  public ProductNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Product not found");
  }
}
