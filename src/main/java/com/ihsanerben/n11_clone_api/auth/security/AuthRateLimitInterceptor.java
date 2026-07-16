package com.ihsanerben.n11_clone_api.auth.security;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import io.github.bucket4j.*;
import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String key = request.getRemoteAddr() + ':' + request.getRequestURI();
    Bucket bucket =
        buckets.computeIfAbsent(
            key,
            ignored ->
                Bucket.builder()
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(5)
                            .refillGreedy(5, Duration.ofMinutes(1))
                            .build())
                    .build());
    if (!bucket.tryConsume(1))
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
    return true;
  }
}
