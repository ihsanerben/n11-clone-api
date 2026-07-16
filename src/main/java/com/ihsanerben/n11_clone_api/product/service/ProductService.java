package com.ihsanerben.n11_clone_api.product.service;

import com.ihsanerben.n11_clone_api.category.entity.Category;
import com.ihsanerben.n11_clone_api.category.exception.CategoryNotFoundException;
import com.ihsanerben.n11_clone_api.category.repository.CategoryRepository;
import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.product.dto.ProductRequest;
import com.ihsanerben.n11_clone_api.product.dto.ProductResponse;
import com.ihsanerben.n11_clone_api.product.entity.Product;
import com.ihsanerben.n11_clone_api.product.exception.ProductNotFoundException;
import com.ihsanerben.n11_clone_api.product.mapper.ProductMapper;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import com.ihsanerben.n11_clone_api.seller.exception.SellerNotFoundException;
import com.ihsanerben.n11_clone_api.seller.repository.SellerRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository products;
  private final SellerRepository sellers;
  private final CategoryRepository categories;
  private final ProductMapper mapper;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional
  public ProductResponse create(ProductRequest request) {
    var seller =
        sellers.findByUserId(authenticatedUser.userId()).orElseThrow(SellerNotFoundException::new);
    Instant now = Instant.now();
    Product p =
        Product.builder()
            .seller(seller)
            .category(findCategory(request.categoryId()))
            .name(request.name().trim())
            .description(normalize(request.description()))
            .price(request.price())
            .stockQuantity(request.stockQuantity())
            .imageUrl(normalize(request.imageUrl()))
            .active(true)
            .createdAt(now)
            .updatedAt(now)
            .build();
    return mapper.toResponse(products.save(p));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product p = owned(id);
    p.setCategory(findCategory(request.categoryId()));
    p.setName(request.name().trim());
    p.setDescription(normalize(request.description()));
    p.setPrice(request.price());
    p.setStockQuantity(request.stockQuantity());
    p.setImageUrl(normalize(request.imageUrl()));
    p.setUpdatedAt(Instant.now());
    return mapper.toResponse(p);
  }

  @Transactional
  public void deactivateOwned(Long id) {
    Product p = owned(id);
    p.setActive(false);
    p.setUpdatedAt(Instant.now());
  }

  @Transactional
  public ProductResponse reactivate(Long id) {
    Product p = owned(id);
    p.setActive(true);
    p.setUpdatedAt(Instant.now());
    return mapper.toResponse(p);
  }

  @Transactional
  public void deactivateAdmin(Long id) {
    Product p = find(id);
    p.setActive(false);
    p.setUpdatedAt(Instant.now());
  }

  @Transactional(readOnly = true)
  public ProductResponse get(Long id) {
    Product p = find(id);
    if (!p.isActive()) throw new ProductNotFoundException();
    return mapper.toResponse(p);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> list(String search, Long categoryId, Pageable pageable) {
    Specification<Product> spec =
        (root, q, cb) -> {
          var predicates = new ArrayList<Predicate>();
          predicates.add(cb.isTrue(root.get("active")));
          if (search != null && !search.isBlank())
            predicates.add(
                cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
          if (categoryId != null)
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    return products.findAll(spec, pageable).map(mapper::toResponse);
  }

  private Product owned(Long id) {
    return products
        .findByIdAndSellerUserId(id, authenticatedUser.userId())
        .orElseThrow(ProductNotFoundException::new);
  }

  private Product find(Long id) {
    return products.findById(id).orElseThrow(ProductNotFoundException::new);
  }

  private Category findCategory(Long id) {
    return categories.findById(id).orElseThrow(CategoryNotFoundException::new);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
