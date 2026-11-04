package territorio_limpo_api.prioridade;

import java.util.List;

import territorio_limpo_api.ocorrencia.NivelPrioridade;

public record PrioridadeResult(NivelPrioridade level, List<PrioridadeReason> reasons, int score) {

	public PrioridadeResult {
		reasons = List.copyOf(reasons);
	}
}
