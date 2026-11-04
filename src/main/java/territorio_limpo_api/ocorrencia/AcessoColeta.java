package territorio_limpo_api.ocorrencia;

public enum AcessoColeta implements EnumRotulado {
	NORMAL("Acesso normal"),
	DIFICIL("Acesso difícil"),
	IMPOSSIVEL_CAMINHAO("Aparentemente impossível para caminhão");

	private final String label;

	AcessoColeta(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
