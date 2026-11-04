package territorio_limpo_api.ocorrencia;

public enum OrigemLocalizacao implements EnumRotulado {
	GPS("GPS do aparelho"),
	MANUAL("Informada manualmente"),
	CONFIRMADA_PELA_EQUIPE("Confirmada pela equipe");

	private final String label;

	OrigemLocalizacao(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
