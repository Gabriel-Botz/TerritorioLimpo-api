package territorio_limpo_api.indicadores;

public record IndicadoresResponse(
		long totalOcorrencias,
		long received,
		long inAnalysis,
		long pendingAnalysis,
		long forwardedOrAwaiting,
		long forwarded,
		long awaitingService,
		long resolved,
		long unresolved,
		long highPrioridadePending,
		long totalEncaminhamentos,
		long encaminhamentosWithProtocol,
		long ocorrenciasWithEncaminhamento) {
}
