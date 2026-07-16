package com.ihsanerben.n11_clone_api.address.service;

import com.ihsanerben.n11_clone_api.address.dto.AddressRequest;
import com.ihsanerben.n11_clone_api.address.dto.AddressResponse;
import com.ihsanerben.n11_clone_api.address.entity.Address;
import com.ihsanerben.n11_clone_api.address.exception.AddressNotFoundException;
import com.ihsanerben.n11_clone_api.address.mapper.AddressMapper;
import com.ihsanerben.n11_clone_api.address.repository.AddressRepository;
import com.ihsanerben.n11_clone_api.common.security.AuthenticatedUserProvider;
import com.ihsanerben.n11_clone_api.user.entity.User;
import com.ihsanerben.n11_clone_api.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {
  private final AddressRepository addresses;
  private final UserRepository users;
  private final AddressMapper mapper;
  private final AuthenticatedUserProvider authenticatedUser;

  @Transactional(readOnly = true)
  public List<AddressResponse> list() {
    return addresses.findAllByUserIdOrderByDefaultAddressDescIdAsc(userId()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AddressResponse get(Long id) {
    return mapper.toResponse(owned(id));
  }

  @Transactional
  public AddressResponse create(AddressRequest request) {
    Long userId = userId();
    boolean makeDefault = request.defaultAddress() || !addresses.existsByUserId(userId);
    if (makeDefault) {
      addresses.clearDefaultForUser(userId);
    }

    User user = users.getReferenceById(userId);
    Address address = Address.builder().user(user).build();
    apply(address, request, makeDefault);
    return mapper.toResponse(addresses.save(address));
  }

  @Transactional
  public AddressResponse update(Long id, AddressRequest request) {
    Long userId = userId();
    Address address = owned(id, userId);
    if (request.defaultAddress()) {
      addresses.clearDefaultForUser(userId);
    }
    apply(address, request, request.defaultAddress());
    return mapper.toResponse(address);
  }

  @Transactional
  public void delete(Long id) {
    Long userId = userId();
    Address address = owned(id, userId);
    boolean wasDefault = address.isDefaultAddress();
    addresses.delete(address);
    addresses.flush();
    if (wasDefault) {
      addresses.findAllByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
          .findFirst()
          .ifPresent(next -> next.setDefaultAddress(true));
    }
  }

  private void apply(Address address, AddressRequest request, boolean defaultAddress) {
    address.setLabel(normalize(request.label()));
    address.setRecipientName(request.recipientName().trim());
    address.setPhone(request.phone().trim());
    address.setAddressLine(request.addressLine().trim());
    address.setCity(request.city().trim());
    address.setPostalCode(request.postalCode().trim());
    address.setDefaultAddress(defaultAddress);
  }

  private Address owned(Long id) {
    return owned(id, userId());
  }

  private Address owned(Long id, Long userId) {
    return addresses.findByIdAndUserId(id, userId).orElseThrow(AddressNotFoundException::new);
  }

  private Long userId() {
    return authenticatedUser.userId();
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
