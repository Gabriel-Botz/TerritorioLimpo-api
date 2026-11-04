package territorio_limpo_api.ocorrencia;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import territorio_limpo_api.common.PaginaResponse;
import territorio_limpo_api.ocorrencia.dto.AdminOcorrenciaDetail;
import territorio_limpo_api.ocorrencia.dto.AdminOcorrenciaListItem;
import territorio_limpo_api.ocorrencia.dto.LocalizacaoConfirmRequest;
import territorio_limpo_api.ocorrencia.dto.ObservacaoCreateRequest;
import territorio_limpo_api.ocorrencia.dto.PrioridadeChangeRequest;
import territorio_limpo_api.ocorrencia.dto.StatusChangeRequest;
import territorio_limpo_api.ocorrencia.dto.VulneravelAreaRequest;
import territorio_limpo_api.encaminhamento.EncaminhamentoService;
import territorio_limpo_api.encaminhamento.dto.EncaminhamentoCriarRequisicao;

@RestController
@RequestMapping("/api/v1/admin/ocorrencias")
@Validated
public class AdminOcorrenciaController {

	private final AdminOcorrenciaService service;
	private final EncaminhamentoService encaminhamentoService;

	public AdminOcorrenciaController(AdminOcorrenciaService service, EncaminhamentoService encaminhamentoService) {
		this.service = service;
		this.encaminhamentoService = encaminhamentoService;
	}

	@GetMapping
	public PaginaResponse<AdminOcorrenciaListItem> list(
			@RequestParam(required = false) OcorrenciaStatus status,
			@RequestParam(required = false) OcorrenciaType type,
			@RequestParam(required = false) NivelPrioridade prioridade,
			@RequestParam(required = false) Boolean hasCoordinates,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) @Size(max = 100, message = "{validation.size}") String q,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "{validation.page.min}") int page,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "{validation.page.size}")
			@Max(value = 100, message = "{validation.page.size}") int size) {

		AdminOcorrenciaFilter filter = new AdminOcorrenciaFilter(status, type, prioridade, hasCoordinates, from, to, q);
		return service.list(filter, page, size);
	}

	@GetMapping("/{id}")
	public AdminOcorrenciaDetail detail(@PathVariable UUID id) {
		return service.detail(id);
	}

	@GetMapping("/{id}/fotos")
	public List<AdminOcorrenciaDetail.FotoView> fotos(@PathVariable UUID id) {
		return service.fotos(id);
	}

	@DeleteMapping("/{id}/fotos/{fotoId}")
	public ResponseEntity<Void> deleteFoto(@PathVariable UUID id, @PathVariable UUID fotoId,
			@AuthenticationPrincipal Jwt jwt) {
		service.deleteFoto(id, fotoId, jwt.getSubject());
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	public AdminOcorrenciaDetail changeStatus(@PathVariable UUID id,
			@Valid @RequestBody StatusChangeRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.changeStatus(id, request, jwt.getSubject());
	}

	@PatchMapping("/{id}/prioridade")
	public AdminOcorrenciaDetail changePrioridade(@PathVariable UUID id,
			@Valid @RequestBody PrioridadeChangeRequest request) {
		return service.changePrioridade(id, request);
	}

	@PatchMapping("/{id}/vulneravel-area")
	public AdminOcorrenciaDetail changeVulneravelArea(@PathVariable UUID id,
			@RequestBody VulneravelAreaRequest request) {
		return service.changeVulneravelArea(id, request);
	}

	@PatchMapping("/{id}/localizacao")
	public AdminOcorrenciaDetail confirmLocalizacao(@PathVariable UUID id,
			@Valid @RequestBody LocalizacaoConfirmRequest request) {
		return service.confirmLocalizacao(id, request);
	}

	@PostMapping("/{id}/observacaos")
	public ResponseEntity<AdminOcorrenciaDetail.ObservacaoView> addObservacao(@PathVariable UUID id,
			@Valid @RequestBody ObservacaoCreateRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		AdminOcorrenciaDetail.ObservacaoView observacao = service.addObservacao(id, request, jwt.getSubject());
		return ResponseEntity.status(HttpStatus.CREATED).body(observacao);
	}

	@GetMapping(value = "/{id}/encaminhamento-text", produces = "text/plain;charset=UTF-8")
	public ResponseEntity<String> encaminhamentoText(@PathVariable UUID id) {
		return ResponseEntity.ok()
				.contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
				.body(encaminhamentoService.generateText(id));
	}

	@PostMapping("/{id}/encaminhamentos")
	public ResponseEntity<AdminOcorrenciaDetail.EncaminhamentoView> createEncaminhamento(@PathVariable UUID id,
			@Valid @RequestBody EncaminhamentoCriarRequisicao request,
			@AuthenticationPrincipal Jwt jwt) {
		AdminOcorrenciaDetail.EncaminhamentoView encaminhamento = encaminhamentoService.create(id, request, jwt.getSubject());
		return ResponseEntity.status(HttpStatus.CREATED).body(encaminhamento);
	}
}
