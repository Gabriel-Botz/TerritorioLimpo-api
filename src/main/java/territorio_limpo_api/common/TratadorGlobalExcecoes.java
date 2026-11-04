package territorio_limpo_api.common;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class TratadorGlobalExcecoes {

	private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExcecoes.class);

	@ExceptionHandler(ExcecaoApi.class)
	public ResponseEntity<ProblemDetail> handleExcecaoApi(ExcecaoApi exception) {
		ProblemDetail problem = problem(exception.getStatus(), exception.getTitle(), exception.getMessage(), "api");
		if (!exception.getErrors().isEmpty()) {
			problem.setProperty("errors", exception.getErrors());
		}
		return ResponseEntity.status(exception.getStatus()).body(problem);
	}

	@ExceptionHandler(ExcecaoRateLimitExcedido.class)
	public ResponseEntity<ProblemDetail> handleRateLimit(ExcecaoRateLimitExcedido exception) {
		ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS, exception.getTitle(),
				exception.getMessage(), "rate-limit");
		problem.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header("Retry-After", Long.toString(exception.getRetryAfterSeconds()))
				.body(problem);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException exception) {
		List<ErroValidacaoCampo> errors = new ArrayList<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.add(new ErroValidacaoCampo(error.getField(), error.getDefaultMessage())));
		exception.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.add(new ErroValidacaoCampo("data", error.getDefaultMessage())));

		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Verifique os campos informados e tente novamente.", "validation");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException exception) {
		List<ErroValidacaoCampo> errors = new ArrayList<>();
		exception.getParameterValidationResults().forEach(result -> {
			String field = result.getMethodParameter().getParameterName();
			if (field == null) {
				field = "parameters";
			}
			String finalField = field;
			result.getResolvableErrors().forEach(error ->
					errors.add(new ErroValidacaoCampo(finalField, error.getDefaultMessage())));
		});
		exception.getCrossParameterValidationResults().forEach(error ->
				errors.add(new ErroValidacaoCampo("parameters", error.getDefaultMessage())));

		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Verifique os parâmetros informados e tente novamente.", "validation");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
		List<ErroValidacaoCampo> errors = exception.getConstraintViolations().stream()
				.map(violation -> new ErroValidacaoCampo(
						violation.getPropertyPath() == null ? "parameters" : violation.getPropertyPath().toString(),
						violation.getMessage()))
				.toList();

		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Verifique os dados informados e tente novamente.", "validation");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Não foi possível ler os dados enviados. Verifique o formato e tente novamente.", "malformed-body"));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ProblemDetail> handleMissingPart(MissingServletRequestPartException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Uma parte obrigatória do formulário não foi enviada.", "missing-part");
		problem.setProperty("errors", List.of(new ErroValidacaoCampo(exception.getRequestPartName(), "Parte obrigatória não enviada.")));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Um parâmetro obrigatório não foi informado.", "missing-parameter");
		problem.setProperty("errors", List.of(new ErroValidacaoCampo(exception.getParameterName(), "Parâmetro obrigatório não informado.")));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
				"Um dos parâmetros informados é inválido.", "invalid-parameter");
		problem.setProperty("errors", List.of(new ErroValidacaoCampo(exception.getName(), "Valor inválido.")));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ProblemDetail> handleMaxUpload(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem(HttpStatus.PAYLOAD_TOO_LARGE,
				"Arquivo muito grande", "O arquivo enviado é muito grande. Envie uma imagem de até 5 MB.", "file-too-large"));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception) {
		log.warn("Conflito de integridade ao gravar dados", exception);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT, "Conflito",
				"Não foi possível concluir a operação por um conflito nos dados. Tente novamente.", "data-conflict"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND, "Não encontrado",
				"O recurso solicitado não existe.", "not-found"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
		String correlationId = UUID.randomUUID().toString();
		log.error("Erro inesperado. correlationId={}", correlationId, exception);
		ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado",
				"Não foi possível concluir a operação. Tente novamente mais tarde.", "internal-error");
		problem.setProperty("correlationId", correlationId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("urn:territorio-limpo:error:" + type));
		return problem;
	}
}
