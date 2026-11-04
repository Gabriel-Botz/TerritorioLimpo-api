package territorio_limpo_api.export;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import territorio_limpo_api.ocorrencia.AdminOcorrenciaFilter;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

@RestController
@RequestMapping("/api/v1/admin/ocorrencias")
@Validated
public class OcorrenciaExportController {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private final OcorrenciaExportService exportService;

	public OcorrenciaExportController(OcorrenciaExportService exportService) {
		this.exportService = exportService;
	}

	@GetMapping(value = "/export.csv", produces = "text/csv")
	public ResponseEntity<StreamingResponseBody> export(
			@RequestParam(required = false) OcorrenciaStatus status,
			@RequestParam(required = false) OcorrenciaType type,
			@RequestParam(required = false) NivelPrioridade prioridade,
			@RequestParam(required = false) Boolean hasCoordinates,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) @Size(max = 100, message = "{validation.size}") String q) {

		AdminOcorrenciaFilter filter = new AdminOcorrenciaFilter(status, type, prioridade, hasCoordinates, from, to, q);
		StreamingResponseBody body = outputStream -> exportService.writeCsv(filter, outputStream);

		String fileName = "ocorrencias-" + LocalDate.now(SAO_PAULO) + ".csv";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
				.body(body);
	}
}
