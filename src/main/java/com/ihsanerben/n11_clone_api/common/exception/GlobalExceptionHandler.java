package com.ihsanerben.n11_clone_api.common.exception;

import com.ihsanerben.n11_clone_api.auth.service.EmailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(EmailDeliveryException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public ErrorResponse handleEmailDelivery(
      EmailDeliveryException exception, HttpServletRequest request) {
    log.error("Email delivery failed while processing path={}", request.getRequestURI(), exception);
    return error(
        HttpStatus.BAD_GATEWAY,
        "Email could not be delivered. Please try again later",
        request,
        null);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleOptimisticLocking(
      ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        "The resource was updated by another request. Please try again",
        request,
        null);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse handleAuthorizationDenied(
      AuthorizationDeniedException exception, HttpServletRequest request) {
    return error(HttpStatus.FORBIDDEN, "Access denied", request, null);
  }

  @ExceptionHandler(ApiException.class)
  public org.springframework.http.ResponseEntity<ErrorResponse> handleApi(
      ApiException exception, HttpServletRequest request) {
    return org.springframework.http.ResponseEntity.status(exception.getStatus())
        .body(error(exception.getStatus(), exception.getMessage(), request, null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleUnexpected(Exception exception, HttpServletRequest request) {
    log.error(
        "Unexpected error while processing request path={}", request.getRequestURI(), exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
  }

  private ErrorResponse error(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      Map<String, String> fieldErrors) {
    return new ErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI(),
        fieldErrors);
  }
}
