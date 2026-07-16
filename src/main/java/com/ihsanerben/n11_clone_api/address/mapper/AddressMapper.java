package com.ihsanerben.n11_clone_api.address.mapper;

import com.ihsanerben.n11_clone_api.address.dto.AddressResponse;
import com.ihsanerben.n11_clone_api.address.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
  public AddressResponse toResponse(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getLabel(),
        address.getRecipientName(),
        address.getPhone(),
        address.getAddressLine(),
        address.getCity(),
        address.getPostalCode(),
        address.isDefaultAddress());
  }
}
