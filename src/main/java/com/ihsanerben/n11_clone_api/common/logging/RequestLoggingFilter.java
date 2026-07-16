package com.ihsanerben.n11_clone_api.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
      log.info(
          "http_request method={} path={} status={} duration_ms={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
    }
  }
}
