package territorio_limpo_api.ocorrencia;

public enum VolumeEstimado implements EnumRotulado {
	PEQUENO("Pequeno"),
	MEDIO("Médio"),
	GRANDE("Grande");

	private final String label;

	VolumeEstimado(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
