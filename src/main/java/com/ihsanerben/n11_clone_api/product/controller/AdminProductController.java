package com.ihsanerben.n11_clone_api.product.controller;

import com.ihsanerben.n11_clone_api.product.dto.ProductResponse;
import com.ihsanerben.n11_clone_api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Products")
public class AdminProductController {
  private final ProductService service;

  @GetMapping
  @Operation(summary = "List all products for moderation with optional filters")
  public Page<ProductResponse> list(
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long categoryId,
      @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return service.listAdmin(search, categoryId, active, pageable);
  }

  @PutMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable Long id) {
    service.deactivateAdmin(id);
  }
}
