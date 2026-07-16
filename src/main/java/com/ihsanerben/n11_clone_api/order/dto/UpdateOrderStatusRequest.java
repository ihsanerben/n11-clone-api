package com.ihsanerben.n11_clone_api.order.dto;

import com.ihsanerben.n11_clone_api.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
