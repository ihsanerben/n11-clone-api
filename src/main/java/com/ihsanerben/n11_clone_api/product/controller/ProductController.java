package com.ihsanerben.n11_clone_api.product.controller;

import com.ihsanerben.n11_clone_api.product.dto.ProductResponse;
import com.ihsanerben.n11_clone_api.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;

  @GetMapping
  public Page<ProductResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long categoryId,
      @ParameterObject Pageable pageable) {
    return service.list(search, categoryId, pageable);
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable Long id) {
    return service.get(id);
  }
}
