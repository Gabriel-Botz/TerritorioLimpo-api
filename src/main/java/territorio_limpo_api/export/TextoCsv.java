package territorio_limpo_api.export;

/**
 * Neutraliza injeção de fórmula em células de texto do CSV, conforme a seção
 * 11 do prompt. Células que começam com =, +, -, @, tab ou retorno de carro
 * recebem um apóstrofo à frente.
 */
public final class TextoCsv {

	private TextoCsv() {
	}

	public static String neutralize(String value) {
		if (value == null || value.isEmpty()) {
			return value == null ? "" : value;
		}
		char first = value.charAt(0);
		if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
			return "'" + value;
		}
		return value;
	}
}
