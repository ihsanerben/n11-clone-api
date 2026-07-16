package com.ihsanerben.n11_clone_api.product.repository;

import com.ihsanerben.n11_clone_api.product.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  Optional<Product> findByIdAndSellerUserId(Long id, Long userId);

  Optional<Product> findByNameAndSellerUserId(String name, Long userId);

  boolean existsByCategoryId(Long categoryId);
}
