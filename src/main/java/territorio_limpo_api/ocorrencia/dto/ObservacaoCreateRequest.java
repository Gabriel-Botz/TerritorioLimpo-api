package territorio_limpo_api.ocorrencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObservacaoCreateRequest(
		@NotBlank(message = "{validation.not-blank}")
		@Size(max = 2000, message = "{validation.size}")
		String body) {
}
