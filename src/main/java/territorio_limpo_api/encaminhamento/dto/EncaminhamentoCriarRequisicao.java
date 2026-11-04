package territorio_limpo_api.encaminhamento.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EncaminhamentoCriarRequisicao(
		@NotBlank(message = "{validation.not-blank}")
		@Size(max = 200, message = "{validation.size}")
		String destination,

		@NotBlank(message = "{validation.not-blank}")
		@Size(max = 100, message = "{validation.size}")
		String channel,

		Instant referredAt,

		@Size(max = 100, message = "{validation.size}")
		String protocolNumber,

		@Size(max = 8000, message = "{validation.size}")
		String requestText) {
}
