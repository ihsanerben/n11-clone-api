package com.ihsanerben.n11_clone_api.address.dto;

public record AddressResponse(
    Long id,
    String label,
    String recipientName,
    String phone,
    String addressLine,
    String city,
    String postalCode,
    boolean defaultAddress) {}
