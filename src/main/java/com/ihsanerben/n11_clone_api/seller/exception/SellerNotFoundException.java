package com.ihsanerben.n11_clone_api.seller.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SellerNotFoundException extends ApiException {
	public SellerNotFoundException() {
		super(HttpStatus.NOT_FOUND, "Seller application not found");
	}
}
