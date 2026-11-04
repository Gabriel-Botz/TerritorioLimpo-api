package territorio_limpo_api.ocorrencia.dto;

import java.time.Instant;

import territorio_limpo_api.ocorrencia.OcorrenciaStatus;

public record OcorrenciaCreatedResponse(
		Long number,
		Instant createdAt,
		String localizacaoSummary,
		OcorrenciaStatus status,
		FotoSummary fotos,
		String message) {

	public record FotoSummary(int received, int saved, int failed) {
	}
}
