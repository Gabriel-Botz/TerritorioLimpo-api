package territorio_limpo_api.abuse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import territorio_limpo_api.config.RateLimitProperties;

class LimitadorEnvioPublicoTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);

	private RateLimitProperties properties(int perIpLimit, int globalLimit) {
		RateLimitProperties properties = new RateLimitProperties();
		properties.setEnabled(true);
		properties.setPerIpLimit(perIpLimit);
		properties.setPerIpWindow(Duration.ofHours(1));
		properties.setGlobalLimit(globalLimit);
		properties.setGlobalWindow(Duration.ofHours(1));
		return properties;
	}

	@Test
	@DisplayName("permite até o limite por IP e bloqueia o excedente com tempo de espera")
	void blocksAfterPerIpLimit() {
		LimitadorEnvioPublico limiter = new LimitadorEnvioPublico(properties(2, 100), clock);

		assertThat(limiter.tryConsume("203.0.113.10")).isEmpty();
		assertThat(limiter.tryConsume("203.0.113.10")).isEmpty();

		assertThat(limiter.tryConsume("203.0.113.10")).isPresent()
				.get()
				.satisfies(seconds -> assertThat(seconds).isPositive());
	}

	@Test
	@DisplayName("IPs diferentes têm limites independentes")
	void differentIpsHaveIndependentLimits() {
		LimitadorEnvioPublico limiter = new LimitadorEnvioPublico(properties(1, 100), clock);

		assertThat(limiter.tryConsume("203.0.113.10")).isEmpty();
		assertThat(limiter.tryConsume("203.0.113.11")).isEmpty();
		assertThat(limiter.tryConsume("203.0.113.10")).isPresent();
	}

	@Test
	@DisplayName("limite global protege mesmo com IPs diferentes")
	void blocksAfterGlobalLimit() {
		LimitadorEnvioPublico limiter = new LimitadorEnvioPublico(properties(10, 2), clock);

		assertThat(limiter.tryConsume("203.0.113.10")).isEmpty();
		assertThat(limiter.tryConsume("203.0.113.11")).isEmpty();
		assertThat(limiter.tryConsume("203.0.113.12")).isPresent();
	}

	@Test
	@DisplayName("quando desabilitado, nunca bloqueia")
	void disabledNeverBlocks() {
		RateLimitProperties properties = properties(1, 1);
		properties.setEnabled(false);
		LimitadorEnvioPublico limiter = new LimitadorEnvioPublico(properties, clock);

		for (int attempt = 0; attempt < 5; attempt++) {
			assertThat(limiter.tryConsume("203.0.113.10")).isEmpty();
		}
	}
}
