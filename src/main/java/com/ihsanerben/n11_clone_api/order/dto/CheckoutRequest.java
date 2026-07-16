package com.ihsanerben.n11_clone_api.order.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull Long addressId) {}
