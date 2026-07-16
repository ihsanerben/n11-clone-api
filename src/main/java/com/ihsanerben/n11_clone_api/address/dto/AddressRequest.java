package com.ihsanerben.n11_clone_api.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @Size(max = 80) String label,
    @NotBlank @Size(max = 150) String recipientName,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[0-9+()\\- ]+$") String phone,
    @NotBlank @Size(max = 500) String addressLine,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 20) String postalCode,
    boolean defaultAddress) {}
