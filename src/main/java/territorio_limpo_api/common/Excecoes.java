package territorio_limpo_api.common;

import java.util.List;

import org.springframework.http.HttpStatus;

public final class Excecoes {

	private Excecoes() {
	}

	public static ExcecaoApi badRequest(String detail) {
		return new ExcecaoApi(HttpStatus.BAD_REQUEST, "Dados inválidos", detail);
	}

	public static ExcecaoApi badRequest(String detail, List<ErroValidacaoCampo> errors) {
		return new ExcecaoApi(HttpStatus.BAD_REQUEST, "Dados inválidos", detail, errors);
	}

	public static ExcecaoApi unauthorized(String detail) {
		return new ExcecaoApi(HttpStatus.UNAUTHORIZED, "Não autenticado", detail);
	}

	public static ExcecaoApi forbidden(String detail) {
		return new ExcecaoApi(HttpStatus.FORBIDDEN, "Acesso negado", detail);
	}

	public static ExcecaoApi notFound(String detail) {
		return new ExcecaoApi(HttpStatus.NOT_FOUND, "Não encontrado", detail);
	}

	public static ExcecaoApi conflict(String detail) {
		return new ExcecaoApi(HttpStatus.CONFLICT, "Conflito", detail);
	}

	public static ExcecaoApi unprocessable(String detail) {
		return new ExcecaoApi(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio não atendida", detail);
	}

	public static ExcecaoApi payloadTooLarge(String detail) {
		return new ExcecaoApi(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo muito grande", detail);
	}

	public static ExcecaoApi serviceUnavailable(String detail) {
		return new ExcecaoApi(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível no momento", detail);
	}
}
