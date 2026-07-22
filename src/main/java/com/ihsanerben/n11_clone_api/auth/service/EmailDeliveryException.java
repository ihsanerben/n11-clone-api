package com.ihsanerben.n11_clone_api.auth.service;

public class EmailDeliveryException extends RuntimeException {
  public EmailDeliveryException(Throwable cause) {
    super("Email could not be delivered", cause);
  }
}
