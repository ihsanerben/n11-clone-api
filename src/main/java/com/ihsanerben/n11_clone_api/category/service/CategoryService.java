package com.ihsanerben.n11_clone_api.category.service;

import com.ihsanerben.n11_clone_api.category.dto.CategoryRequest;
import com.ihsanerben.n11_clone_api.category.dto.CategoryResponse;
import com.ihsanerben.n11_clone_api.category.entity.Category;
import com.ihsanerben.n11_clone_api.category.exception.CategoryNotFoundException;
import com.ihsanerben.n11_clone_api.category.mapper.CategoryMapper;
import com.ihsanerben.n11_clone_api.category.repository.CategoryRepository;
import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import com.ihsanerben.n11_clone_api.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final CategoryMapper mapper;

  @Transactional(readOnly = true)
  public List<CategoryResponse> list() {
    return categories.findAll().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public CategoryResponse get(Long id) {
    return mapper.toResponse(find(id));
  }

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    String name = request.name().trim();
    if (categories.existsByNameIgnoreCase(name))
      throw new ApiException(HttpStatus.CONFLICT, "Category name is already in use");
    return mapper.toResponse(
        categories.save(
            Category.builder().name(name).description(normalize(request.description())).build()));
  }

  @Transactional
  public CategoryResponse update(Long id, CategoryRequest request) {
    Category category = find(id);
    String name = request.name().trim();
    if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
      throw new ApiException(HttpStatus.CONFLICT, "Category name is already in use");
    }
    category.setName(name);
    category.setDescription(normalize(request.description()));
    return mapper.toResponse(category);
  }

  @Transactional
  public void delete(Long id) {
    Category category = find(id);
    if (products.existsByCategoryId(id)) {
      throw new ApiException(HttpStatus.CONFLICT, "Category has associated products");
    }
    categories.delete(category);
  }

  public Category find(Long id) {
    return categories.findById(id).orElseThrow(CategoryNotFoundException::new);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
