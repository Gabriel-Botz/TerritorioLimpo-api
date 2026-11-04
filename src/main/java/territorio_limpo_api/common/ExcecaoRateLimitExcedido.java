package territorio_limpo_api.common;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ExcecaoRateLimitExcedido extends ExcecaoApi {

	private final long retryAfterSeconds;

	public ExcecaoRateLimitExcedido(long retryAfterSeconds, String detail) {
		super(HttpStatus.TOO_MANY_REQUESTS, "Muitos envios", detail, List.of());
		this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
