package com.ihsanerben.n11_clone_api.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {
	public Long userId() {
		JwtAuthenticationToken authentication =
				(JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Number userId = authentication.getToken().getClaim("userId");
		return userId.longValue();
	}
}
