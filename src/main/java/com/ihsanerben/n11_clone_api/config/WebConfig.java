package com.ihsanerben.n11_clone_api.config;
import com.ihsanerben.n11_clone_api.auth.security.AuthRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration @RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final AuthRateLimitInterceptor interceptor;
	@Override public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(interceptor).addPathPatterns(
				"/api/auth/login", "/api/auth/register", "/api/auth/forgot-password");
	}
}
