package com.ihsanerben.n11_clone_api.seller.repository;

import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
	Optional<Seller> findByUserId(Long userId);
	Page<Seller> findAllByStatus(SellerStatus status, Pageable pageable);
}
