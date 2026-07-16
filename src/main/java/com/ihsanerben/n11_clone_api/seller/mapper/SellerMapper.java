package com.ihsanerben.n11_clone_api.seller.mapper;

import com.ihsanerben.n11_clone_api.seller.dto.SellerResponse;
import com.ihsanerben.n11_clone_api.seller.entity.Seller;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {
	public SellerResponse toResponse(Seller seller) {
		return new SellerResponse(
				seller.getId(),
				seller.getUser().getId(),
				seller.getUser().getUsername(),
				seller.getUser().getEmail(),
				seller.getStoreName(),
				seller.getDescription(),
				seller.getStatus(),
				seller.getAppliedAt(),
				seller.getReviewedAt()
		);
	}
}
