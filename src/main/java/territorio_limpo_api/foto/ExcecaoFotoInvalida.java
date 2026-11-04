package territorio_limpo_api.foto;

public class ExcecaoFotoInvalida extends RuntimeException {

	public ExcecaoFotoInvalida(String message) {
		super(message);
	}

	public ExcecaoFotoInvalida(String message, Throwable cause) {
		super(message, cause);
	}
}
