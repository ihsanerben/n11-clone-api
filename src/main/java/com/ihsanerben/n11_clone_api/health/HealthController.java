package com.ihsanerben.n11_clone_api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  @GetMapping
  @Operation(summary = "Check API health")
  @ApiResponse(responseCode = "200", description = "API is running")
  public HealthResponse health() {
    return new HealthResponse("UP");
  }
}
