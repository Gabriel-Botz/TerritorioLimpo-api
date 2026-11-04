package territorio_limpo_api.common;

public final class NormalizadorTexto {

	private NormalizadorTexto() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}

	public static boolean hasControlCharacters(String value) {
		if (value == null) {
			return false;
		}
		return value.codePoints()
				.anyMatch(codePoint -> Character.isISOControl(codePoint)
						&& codePoint != '\n' && codePoint != '\r' && codePoint != '\t');
	}

	public static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
