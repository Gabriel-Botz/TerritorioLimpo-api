package territorio_limpo_api.ocorrencia;

public enum RiscoDrenagem implements EnumRotulado {
	SIM("Sim"),
	NAO("Não"),
	NAO_SEI("Não sei");

	private final String label;

	RiscoDrenagem(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
