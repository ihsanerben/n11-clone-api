package com.ihsanerben.n11_clone_api.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

	private final HealthController healthController = new HealthController();

	@Test
	void shouldReturnUpStatus() {
		HealthResponse response = healthController.health();

		assertThat(response.status()).isEqualTo("UP");
	}
}
