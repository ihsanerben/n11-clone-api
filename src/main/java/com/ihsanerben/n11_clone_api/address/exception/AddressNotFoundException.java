package com.ihsanerben.n11_clone_api.address.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AddressNotFoundException extends ApiException {
  public AddressNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Address not found");
  }
}
