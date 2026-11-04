package territorio_limpo_api.ocorrencia;

public enum NivelPrioridade implements EnumRotulado {
	BAIXA("Baixa"),
	MEDIA("Média"),
	ALTA("Alta");

	private final String label;

	NivelPrioridade(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
