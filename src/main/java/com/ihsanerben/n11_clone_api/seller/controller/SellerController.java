package com.ihsanerben.n11_clone_api.seller.controller;

import com.ihsanerben.n11_clone_api.seller.dto.SellerApplicationRequest;
import com.ihsanerben.n11_clone_api.seller.dto.SellerResponse;
import com.ihsanerben.n11_clone_api.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {
	private final SellerService sellerService;

	@PostMapping("/apply")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('USER')")
	@Operation(summary = "Apply to become a seller or resubmit a rejected application")
	public SellerResponse apply(@Valid @RequestBody SellerApplicationRequest request) {
		return sellerService.apply(request);
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('USER', 'SELLER')")
	@Operation(summary = "Get the current user's seller application")
	public SellerResponse getMyApplication() {
		return sellerService.getMyApplication();
	}
}
