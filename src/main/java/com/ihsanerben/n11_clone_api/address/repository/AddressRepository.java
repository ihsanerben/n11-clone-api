package com.ihsanerben.n11_clone_api.address.repository;

import com.ihsanerben.n11_clone_api.address.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AddressRepository extends JpaRepository<Address, Long> {
  List<Address> findAllByUserIdOrderByDefaultAddressDescIdAsc(Long userId);

  Optional<Address> findByIdAndUserId(Long id, Long userId);

  boolean existsByUserId(Long userId);

  @Modifying
  @Query(
      "update Address address set address.defaultAddress = false where address.user.id = :userId")
  void clearDefaultForUser(Long userId);
}
