package territorio_limpo_api.ocorrencia.dto;

import java.time.Instant;
import java.util.UUID;

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

public record AdminOcorrenciaListItem(
		UUID id,
		Long number,
		Instant createdAt,
		OcorrenciaType type,
		VolumeEstimado estimatedVolume,
		AcessoColeta collectionAcesso,
		RiscoDrenagem drenagemRisk,
		OcorrenciaStatus status,
		NivelPrioridade prioridade,
		boolean prioridadeSetManually,
		String addressText,
		String referenciaPoint,
		boolean hasCoordinates,
		long fotoCount) {
}
