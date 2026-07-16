package com.ihsanerben.n11_clone_api.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@Min(1) int quantity) {}
