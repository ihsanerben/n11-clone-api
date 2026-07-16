package com.ihsanerben.n11_clone_api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank @Size(max = 120) String name, @Size(max = 500) String description) {}
