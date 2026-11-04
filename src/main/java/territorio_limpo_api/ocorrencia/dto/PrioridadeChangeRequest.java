package territorio_limpo_api.ocorrencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

public record PrioridadeChangeRequest(
		@NotNull(message = "{validation.prioridade.required}")
		NivelPrioridade prioridade,

		@Size(max = 500, message = "{validation.size}")
		String reason) {
}
