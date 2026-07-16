package com.ihsanerben.n11_clone_api.seller.controller;

import com.ihsanerben.n11_clone_api.seller.dto.SellerResponse;
import com.ihsanerben.n11_clone_api.seller.entity.SellerStatus;
import com.ihsanerben.n11_clone_api.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sellers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSellerController {
	private final SellerService sellerService;

	@GetMapping
	@Operation(summary = "List seller applications")
	public Page<SellerResponse> list(
			@RequestParam(required = false) SellerStatus status,
			@ParameterObject Pageable pageable
	) {
		return sellerService.list(status, pageable);
	}

	@PutMapping("/{sellerId}/approve")
	@Operation(summary = "Approve a pending seller application")
	public SellerResponse approve(@PathVariable Long sellerId) {
		return sellerService.approve(sellerId);
	}

	@PutMapping("/{sellerId}/reject")
	@Operation(summary = "Reject a pending seller application")
	public SellerResponse reject(@PathVariable Long sellerId) {
		return sellerService.reject(sellerId);
	}
}
