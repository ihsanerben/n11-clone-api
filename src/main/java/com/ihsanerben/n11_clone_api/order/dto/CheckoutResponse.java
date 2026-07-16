package com.ihsanerben.n11_clone_api.order.dto;

import java.util.List;

public record CheckoutResponse(List<CreatedOrderResponse> orders) {}
