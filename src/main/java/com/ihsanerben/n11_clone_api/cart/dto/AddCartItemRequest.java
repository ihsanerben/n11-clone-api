package com.ihsanerben.n11_clone_api.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(@NotNull Long productId, @Min(1) int quantity) {}
