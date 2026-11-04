package territorio_limpo_api.prioridade;

import territorio_limpo_api.ocorrencia.EnumRotulado;

public enum PrioridadeReason implements EnumRotulado {
	DRENAGEM_COM_RISCO("Bueiro obstruído ou água acumulada"),
	DRENAGEM_INCERTA("Situação de drenagem não confirmada"),
	POSSIVEL_OBSTRUCAO("Tipo relacionado a obstrução de drenagem"),
	VOLUME_GRANDE("Volume grande de resíduos"),
	VOLUME_MEDIO("Volume médio de resíduos"),
	ACESSO_DIFICIL("Acesso difícil para coleta"),
	ACESSO_IMPOSSIVEL_CAMINHAO("Acesso aparentemente impossível para caminhão"),
	AREA_VULNERAVEL_VERIFICADA("Área vulnerável verificada pela equipe");

	private final String label;

	PrioridadeReason(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

	public static PrioridadeReason fromCode(String code) {
		try {
			return PrioridadeReason.valueOf(code);
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			return null;
		}
	}
}
