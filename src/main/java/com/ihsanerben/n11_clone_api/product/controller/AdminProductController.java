package com.ihsanerben.n11_clone_api.product.controller;

import com.ihsanerben.n11_clone_api.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
  private final ProductService service;

  @PutMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable Long id) {
    service.deactivateAdmin(id);
  }
}
