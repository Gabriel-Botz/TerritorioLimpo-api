package territorio_limpo_api.ocorrencia.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

public record AdminOcorrenciaDetail(
		UUID id,
		Long number,
		Instant createdAt,
		Instant updatedAt,
		OcorrenciaType type,
		VolumeEstimado estimatedVolume,
		AcessoColeta collectionAcesso,
		RiscoDrenagem drenagemRisk,
		String observacaos,
		BigDecimal latitude,
		BigDecimal longitude,
		BigDecimal precisaoMeters,
		String addressText,
		String referenciaPoint,
		OrigemLocalizacao localizacaoSource,
		OcorrenciaStatus status,
		NivelPrioridade prioridade,
		List<PrioridadeReasonView> prioridadeReasons,
		boolean prioridadeSetManually,
		String manualPrioridadeReason,
		Boolean inVulneravelArea,
		Instant resolvedAt,
		String resolvedConfirmedBy,
		String resolutionObservacao,
		List<HistoricoView> historico,
		List<EncaminhamentoView> encaminhamentos,
		List<ObservacaoView> observacaosInternal,
		List<FotoView> fotos) {

	public record PrioridadeReasonView(String code, String label) {
	}

	public record HistoricoView(
			OcorrenciaStatus fromStatus,
			OcorrenciaStatus toStatus,
			String changedBy,
			String observacao,
			Instant changedAt) {
	}

	public record EncaminhamentoView(
			Long id,
			String destination,
			String channel,
			Instant referredAt,
			String protocolNumber,
			String requestText,
			String referredBy,
			Instant createdAt) {
	}

	public record ObservacaoView(Long id, String authorId, String body, Instant createdAt) {
	}

	public record FotoView(UUID id, String url, Instant createdAt) {
	}
}
