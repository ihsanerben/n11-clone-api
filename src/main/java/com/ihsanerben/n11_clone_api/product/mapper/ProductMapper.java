package com.ihsanerben.n11_clone_api.product.mapper;

import com.ihsanerben.n11_clone_api.product.dto.ProductResponse;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
  public ProductResponse toResponse(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getName(),
        p.getDescription(),
        p.getPrice(),
        p.getStockQuantity(),
        p.getImageUrl(),
        p.isActive(),
        p.getVersion(),
        p.getCategory().getId(),
        p.getCategory().getName(),
        p.getSeller().getId(),
        p.getSeller().getStoreName(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
