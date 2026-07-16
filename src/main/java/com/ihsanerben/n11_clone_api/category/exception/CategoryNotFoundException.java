package com.ihsanerben.n11_clone_api.category.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends ApiException {
  public CategoryNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Category not found");
  }
}
