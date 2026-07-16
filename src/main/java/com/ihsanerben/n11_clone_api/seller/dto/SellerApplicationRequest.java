package com.ihsanerben.n11_clone_api.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
    @NotBlank @Size(max = 120) String storeName, @Size(max = 1000) String description) {}
