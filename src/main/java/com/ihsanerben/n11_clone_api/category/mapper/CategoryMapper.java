package com.ihsanerben.n11_clone_api.category.mapper;

import com.ihsanerben.n11_clone_api.category.dto.CategoryResponse;
import com.ihsanerben.n11_clone_api.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
  public CategoryResponse toResponse(Category category) {
    return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
  }
}
