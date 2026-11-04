package territorio_limpo_api.ocorrencia;

public enum OcorrenciaStatus implements EnumRotulado {
	RECEBIDA("Recebida"),
	EM_ANALISE("Em análise"),
	ENCAMINHADA("Encaminhada ao serviço responsável"),
	AGUARDANDO_ATENDIMENTO("Aguardando atendimento"),
	RESOLVIDA("Resolvida"),
	NAO_SOLUCIONADA("Não solucionada ou encaminhamento necessário");

	private final String label;

	OcorrenciaStatus(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
