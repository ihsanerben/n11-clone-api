package com.ihsanerben.n11_clone_api.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerIT {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("app.cors.allowed-origin", () -> "http://localhost:5173");
		registry.add("app.auth.jwt-secret", () -> "integration-test-secret-key-at-least-32-characters");
		registry.add("app.auth.access-expiration-ms", () -> "1200000");
		registry.add("app.auth.refresh-expiration-days", () -> "14");
		registry.add("app.auth.verification-expiration-hours", () -> "24");
		registry.add("app.auth.refresh-cookie-name", () -> "refreshToken");
		registry.add("app.auth.cookie-secure", () -> "false");
		registry.add("app.email.api-key", () -> "test-key");
		registry.add("app.email.from", () -> "test@example.com");
		registry.add("app.email.frontend-base-url", () -> "http://localhost:5173");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@Test
	void shouldExposePublicHealthEndpoint() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"status":"UP"}
						"""));
	}

	@Test
	void shouldApplyFlywayMigrations() {
		Integer appliedMigrations = jdbcClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success")
				.query(Integer.class)
				.single();

		assertThat(appliedMigrations).isEqualTo(2);
	}
}
