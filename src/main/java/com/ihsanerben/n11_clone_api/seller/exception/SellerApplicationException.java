package com.ihsanerben.n11_clone_api.seller.exception;

import com.ihsanerben.n11_clone_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SellerApplicationException extends ApiException {
	public SellerApplicationException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
