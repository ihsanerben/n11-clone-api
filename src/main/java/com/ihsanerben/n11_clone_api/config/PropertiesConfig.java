package com.ihsanerben.n11_clone_api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class PropertiesConfig {
}
