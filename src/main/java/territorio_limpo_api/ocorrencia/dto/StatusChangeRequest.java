package territorio_limpo_api.ocorrencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;

public record StatusChangeRequest(
		@NotNull(message = "{validation.status.required}")
		OcorrenciaStatus status,

		@Size(max = 2000, message = "{validation.size}")
		String observacao) {
}
