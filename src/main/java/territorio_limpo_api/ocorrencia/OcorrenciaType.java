package territorio_limpo_api.ocorrencia;

public enum OcorrenciaType implements EnumRotulado {
	LIXO_COMUM("Lixo comum acumulado"),
	ENTULHO("Entulho ou restos de obra"),
	VOLUMOSO("Móveis ou objetos volumosos descartados"),
	DRENAGEM_OBSTRUIDA("Possível obstrução de bueiro ou drenagem"),
	OUTRO("Outro");

	private final String label;

	OcorrenciaType(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
