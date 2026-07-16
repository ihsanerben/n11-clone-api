package com.ihsanerben.n11_clone_api.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @NotNull @Positive BigDecimal price,
    @PositiveOrZero int stockQuantity,
    @NotNull Long categoryId,
    @Size(max = 1000) String imageUrl) {}
