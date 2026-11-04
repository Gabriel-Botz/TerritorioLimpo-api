package territorio_limpo_api.abuse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import territorio_limpo_api.config.RateLimitProperties;

/**
 * Rate limit em memória (sem Redis). As chaves são IPs e expiram; nenhum IP é
 * persistido ou registrado em log.
 */
@Component
public class LimitadorEnvioPublico {

	private static final int CLEANUP_THRESHOLD = 10_000;

	private final RateLimitProperties properties;
	private final Clock clock;
	private final Map<String, Entry> perIpBuckets = new ConcurrentHashMap<>();
	private final Bucket globalBucket;

	public LimitadorEnvioPublico(RateLimitProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.globalBucket = bucket(properties.getGlobalLimit(), properties.getGlobalWindow());
	}

	/**
	 * @return vazio quando o envio é permitido; caso contrário, os segundos
	 * sugeridos em {@code Retry-After}.
	 */
	public Optional<Long> tryConsume(String clientIp) {
		if (!properties.isEnabled()) {
			return Optional.empty();
		}

		cleanupIfNeeded();

		Entry entry = perIpBuckets.compute(clientIp, (key, current) -> {
			Entry chosen = current == null ? new Entry(bucket(properties.getPerIpLimit(),
					properties.getPerIpWindow()), clock.instant()) : current;
			return new Entry(chosen.bucket(), clock.instant());
		});

		ConsumptionProbe perIp = entry.bucket().tryConsumeAndReturnRemaining(1);
		if (!perIp.isConsumed()) {
			return Optional.of(secondsToWait(perIp));
		}

		ConsumptionProbe global = globalBucket.tryConsumeAndReturnRemaining(1);
		if (!global.isConsumed()) {
			return Optional.of(secondsToWait(global));
		}

		return Optional.empty();
	}

	private Bucket bucket(int capacity, Duration window) {
		return Bucket.builder()
				.addLimit(Bandwidth.builder()
						.capacity(capacity)
						.refillGreedy(capacity, window)
						.build())
				.build();
	}

	private long secondsToWait(ConsumptionProbe probe) {
		long nanos = probe.getNanosToWaitForRefill();
		return Math.max(1, Duration.ofNanos(nanos).toSeconds() + 1);
	}

	private void cleanupIfNeeded() {
		if (perIpBuckets.size() < CLEANUP_THRESHOLD) {
			return;
		}
		Instant threshold = clock.instant().minus(properties.getEntryTtl());
		perIpBuckets.entrySet().removeIf(entry -> entry.getValue().lastUsed().isBefore(threshold));
	}

	private record Entry(Bucket bucket, Instant lastUsed) {
	}
}
