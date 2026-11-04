package territorio_limpo_api.common;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ExcecaoApi extends RuntimeException {

	private final HttpStatus status;
	private final String title;
	private final List<ErroValidacaoCampo> errors;

	public ExcecaoApi(HttpStatus status, String title, String detail) {
		this(status, title, detail, List.of());
	}

	public ExcecaoApi(HttpStatus status, String title, String detail, List<ErroValidacaoCampo> errors) {
		super(detail);
		this.status = status;
		this.title = title;
		this.errors = errors == null ? List.of() : List.copyOf(errors);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getTitle() {
		return title;
	}

	public List<ErroValidacaoCampo> getErrors() {
		return errors;
	}
}
