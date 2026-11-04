package territorio_limpo_api.ocorrencia;

import java.time.LocalDate;

public record AdminOcorrenciaFilter(
		OcorrenciaStatus status,
		OcorrenciaType type,
		NivelPrioridade prioridade,
		Boolean hasCoordinates,
		LocalDate from,
		LocalDate to,
		String query) {
}
