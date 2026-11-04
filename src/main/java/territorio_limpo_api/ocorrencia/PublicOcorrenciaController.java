package territorio_limpo_api.ocorrencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import territorio_limpo_api.abuse.ResolvedorIpCliente;
import territorio_limpo_api.abuse.LimitadorEnvioPublico;
import territorio_limpo_api.abuse.VerificadorTurnstile;
import territorio_limpo_api.common.Excecoes;
import territorio_limpo_api.common.ErroValidacaoCampo;
import territorio_limpo_api.common.ExcecaoRateLimitExcedido;
import territorio_limpo_api.ocorrencia.dto.OcorrenciaCreatedResponse;
import territorio_limpo_api.ocorrencia.dto.PublicOcorrenciaRequest;

@RestController
@RequestMapping("/api/v1/public")
public class PublicOcorrenciaController {

	private final PublicOcorrenciaService service;
	private final LimitadorEnvioPublico rateLimiter;
	private final ResolvedorIpCliente clientIpResolver;
	private final VerificadorTurnstile turnstileVerifier;

	public PublicOcorrenciaController(PublicOcorrenciaService service,
			LimitadorEnvioPublico rateLimiter,
			ResolvedorIpCliente clientIpResolver,
			VerificadorTurnstile turnstileVerifier) {
		this.service = service;
		this.rateLimiter = rateLimiter;
		this.clientIpResolver = clientIpResolver;
		this.turnstileVerifier = turnstileVerifier;
	}

	@PostMapping(path = "/ocorrencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OcorrenciaCreatedResponse> create(
			@RequestPart("data") @Valid PublicOcorrenciaRequest data,
			@RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken,
			HttpServletRequest request) {

		rateLimiter.tryConsume(clientIpResolver.resolve(request)).ifPresent(retryAfter -> {
			throw new ExcecaoRateLimitExcedido(retryAfter,
					"Você fez muitos envios em pouco tempo. Aguarde alguns minutos e tente novamente.");
		});

		turnstileVerifier.verify(turnstileToken);

		UUID requestId = parseIdempotencyKey(idempotencyKey);
		try {
			PublicOcorrenciaService.CreationResult result = service.create(data, fotos, requestId);
			HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
			return ResponseEntity.status(status).body(result.response());
		}
		catch (DataIntegrityViolationException exception) {
			if (requestId == null) {
				throw exception;
			}
			Optional<OcorrenciaCreatedResponse> replay = service.findReplay(requestId);
			if (replay.isPresent()) {
				return ResponseEntity.ok(replay.get());
			}
			throw exception;
		}
	}

	private UUID parseIdempotencyKey(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value.trim());
		}
		catch (IllegalArgumentException exception) {
			throw Excecoes.badRequest("O identificador de reenvio é inválido.",
					List.of(new ErroValidacaoCampo("Idempotency-Key", "Informe um UUID válido.")));
		}
	}
}
