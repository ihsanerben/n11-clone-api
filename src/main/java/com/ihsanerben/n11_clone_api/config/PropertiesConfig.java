package com.ihsanerben.n11_clone_api.config;

import com.ihsanerben.n11_clone_api.auth.config.AuthProperties;
import com.ihsanerben.n11_clone_api.auth.config.EmailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CorsProperties.class, AuthProperties.class, EmailProperties.class})
public class PropertiesConfig {
}
