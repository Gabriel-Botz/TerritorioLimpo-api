package territorio_limpo_api.encaminhamento;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import territorio_limpo_api.common.Excecoes;
import territorio_limpo_api.common.ErroValidacaoCampo;
import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.dto.AdminOcorrenciaDetail;
import territorio_limpo_api.encaminhamento.dto.EncaminhamentoCriarRequisicao;

@Service
public class EncaminhamentoService {

	private final OcorrenciaRepository ocorrenciaRepository;
	private final OcorrenciaEncaminhamentoRepository encaminhamentoRepository;
	private final EncaminhamentoTextGenerator textGenerator;
	private final Clock clock;

	public EncaminhamentoService(OcorrenciaRepository ocorrenciaRepository,
			OcorrenciaEncaminhamentoRepository encaminhamentoRepository,
			EncaminhamentoTextGenerator textGenerator,
			Clock clock) {
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.encaminhamentoRepository = encaminhamentoRepository;
		this.textGenerator = textGenerator;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public String generateText(UUID ocorrenciaId) {
		return textGenerator.generate(find(ocorrenciaId));
	}

	@Transactional
	public AdminOcorrenciaDetail.EncaminhamentoView create(UUID ocorrenciaId, EncaminhamentoCriarRequisicao request,
			String adminId) {
		Ocorrencia ocorrencia = find(ocorrenciaId);
		String destination = required(request.destination(), "destination");
		String channel = required(request.channel(), "channel");
		String protocolNumber = optional(request.protocolNumber(), "protocolNumber");
		String requestText = optional(request.requestText(), "requestText");
		if (requestText == null) {
			requestText = textGenerator.generate(ocorrencia);
		}

		Instant now = clock.instant();
		Instant referredAt = request.referredAt() == null ? now : request.referredAt();
		OcorrenciaEncaminhamento encaminhamento = encaminhamentoRepository.saveAndFlush(OcorrenciaEncaminhamento.create(ocorrencia,
				destination, channel, referredAt, protocolNumber, requestText, adminId, now));

		return new AdminOcorrenciaDetail.EncaminhamentoView(encaminhamento.getId(), encaminhamento.getDestino(),
				encaminhamento.getCanal(), encaminhamento.getEncaminhadoEm(), encaminhamento.getNumeroProtocolo(),
				encaminhamento.getTextoSolicitacao(), encaminhamento.getEncaminhadoPor(), encaminhamento.getCriadoEm());
	}

	private Ocorrencia find(UUID ocorrenciaId) {
		return ocorrenciaRepository.findById(ocorrenciaId)
				.orElseThrow(() -> Excecoes.notFound("Ocorrência não encontrada."));
	}

	private String required(String value, String field) {
		String normalized = optional(value, field);
		if (normalized == null) {
			throw Excecoes.badRequest("Este campo não pode ficar em branco.",
					List.of(new ErroValidacaoCampo(field, "Este campo não pode ficar em branco.")));
		}
		return normalized;
	}

	private String optional(String value, String field) {
		if (NormalizadorTexto.hasControlCharacters(value)) {
			throw Excecoes.badRequest("Remova caracteres inválidos do texto.",
					List.of(new ErroValidacaoCampo(field, "Remova caracteres inválidos do texto.")));
		}
		return NormalizadorTexto.normalize(value);
	}
}
