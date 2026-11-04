package territorio_limpo_api.storage;

public class ExcecaoStorage extends RuntimeException {

	public ExcecaoStorage(String message) {
		super(message);
	}

	public ExcecaoStorage(String message, Throwable cause) {
		super(message, cause);
	}
}
