package territorio_limpo_api.prioridade;

import java.util.ArrayList;
import java.util.List;

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

/**
 * Regras transparentes de prioridade. A prioridade é apenas um apoio à
 * avaliação humana: não representa risco geológico nem previsão de desastres.
 */
public final class PrioridadeCalculator {

	public static final int DRAINAGE_RISK_YES_POINTS = 3;
	public static final int DRAINAGE_RISK_UNKNOWN_POINTS = 1;
	public static final int OBSTRUCTION_TYPE_POINTS = 2;
	public static final int LARGE_VOLUME_POINTS = 2;
	public static final int MEDIUM_VOLUME_POINTS = 1;
	public static final int DIFFICULT_ACCESS_POINTS = 1;
	public static final int IMPOSSIBLE_ACCESS_POINTS = 2;
	public static final int VULNERABLE_AREA_POINTS = 2;

	public static final int HIGH_PRIORITY_THRESHOLD = 5;
	public static final int MEDIUM_PRIORITY_THRESHOLD = 3;

	private PrioridadeCalculator() {
	}

	public static PrioridadeResult calculate(
			OcorrenciaType type,
			VolumeEstimado estimatedVolume,
			AcessoColeta collectionAcesso,
			RiscoDrenagem drenagemRisk,
			boolean inVulneravelArea) {

		int score = 0;
		List<PrioridadeReason> reasons = new ArrayList<>();

		if (drenagemRisk == RiscoDrenagem.SIM) {
			score += DRAINAGE_RISK_YES_POINTS;
			reasons.add(PrioridadeReason.DRENAGEM_COM_RISCO);
		}
		else if (drenagemRisk == RiscoDrenagem.NAO_SEI) {
			score += DRAINAGE_RISK_UNKNOWN_POINTS;
			reasons.add(PrioridadeReason.DRENAGEM_INCERTA);
		}

		if (type == OcorrenciaType.DRENAGEM_OBSTRUIDA) {
			score += OBSTRUCTION_TYPE_POINTS;
			reasons.add(PrioridadeReason.POSSIVEL_OBSTRUCAO);
		}

		if (estimatedVolume == VolumeEstimado.GRANDE) {
			score += LARGE_VOLUME_POINTS;
			reasons.add(PrioridadeReason.VOLUME_GRANDE);
		}
		else if (estimatedVolume == VolumeEstimado.MEDIO) {
			score += MEDIUM_VOLUME_POINTS;
			reasons.add(PrioridadeReason.VOLUME_MEDIO);
		}

		if (collectionAcesso == AcessoColeta.IMPOSSIVEL_CAMINHAO) {
			score += IMPOSSIBLE_ACCESS_POINTS;
			reasons.add(PrioridadeReason.ACESSO_IMPOSSIVEL_CAMINHAO);
		}
		else if (collectionAcesso == AcessoColeta.DIFICIL) {
			score += DIFFICULT_ACCESS_POINTS;
			reasons.add(PrioridadeReason.ACESSO_DIFICIL);
		}

		if (inVulneravelArea) {
			score += VULNERABLE_AREA_POINTS;
			reasons.add(PrioridadeReason.AREA_VULNERAVEL_VERIFICADA);
		}

		return new PrioridadeResult(levelFor(score), reasons, score);
	}

	public static NivelPrioridade levelFor(int score) {
		if (score >= HIGH_PRIORITY_THRESHOLD) {
			return NivelPrioridade.ALTA;
		}
		if (score >= MEDIUM_PRIORITY_THRESHOLD) {
			return NivelPrioridade.MEDIA;
		}
		return NivelPrioridade.BAIXA;
	}
}
