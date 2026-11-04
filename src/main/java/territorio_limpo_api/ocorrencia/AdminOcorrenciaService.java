package territorio_limpo_api.ocorrencia;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import territorio_limpo_api.common.Excecoes;
import territorio_limpo_api.common.ErroValidacaoCampo;
import territorio_limpo_api.common.PaginaResponse;
import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.historico.OcorrenciaStatusHistorico;
import territorio_limpo_api.historico.OcorrenciaStatusHistoricoRepository;
import territorio_limpo_api.observacao.OcorrenciaInternalObservacao;
import territorio_limpo_api.observacao.OcorrenciaInternalObservacaoRepository;
import territorio_limpo_api.ocorrencia.dto.AdminOcorrenciaDetail;
import territorio_limpo_api.ocorrencia.dto.AdminOcorrenciaListItem;
import territorio_limpo_api.ocorrencia.dto.LocalizacaoConfirmRequest;
import territorio_limpo_api.ocorrencia.dto.ObservacaoCreateRequest;
import territorio_limpo_api.ocorrencia.dto.PrioridadeChangeRequest;
import territorio_limpo_api.ocorrencia.dto.StatusChangeRequest;
import territorio_limpo_api.ocorrencia.dto.VulneravelAreaRequest;
import territorio_limpo_api.foto.OcorrenciaFoto;
import territorio_limpo_api.foto.OcorrenciaFotoRepository;
import territorio_limpo_api.prioridade.PrioridadeCalculator;
import territorio_limpo_api.prioridade.PrioridadeReason;
import territorio_limpo_api.prioridade.PrioridadeResult;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamento;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamentoRepository;
import territorio_limpo_api.storage.ClienteStorage;
import territorio_limpo_api.storage.ExcecaoStorage;

import java.util.ArrayList;

@Service
public class AdminOcorrenciaService {

	private static final Logger log = LoggerFactory.getLogger(AdminOcorrenciaService.class);

	private final OcorrenciaRepository ocorrenciaRepository;
	private final OcorrenciaFotoRepository fotoRepository;
	private final OcorrenciaStatusHistoricoRepository historicoRepository;
	private final OcorrenciaEncaminhamentoRepository encaminhamentoRepository;
	private final OcorrenciaInternalObservacaoRepository observacaoRepository;
	private final ClienteStorage storageClient;
	private final Clock clock;

	public AdminOcorrenciaService(OcorrenciaRepository ocorrenciaRepository,
			OcorrenciaFotoRepository fotoRepository,
			OcorrenciaStatusHistoricoRepository historicoRepository,
			OcorrenciaEncaminhamentoRepository encaminhamentoRepository,
			OcorrenciaInternalObservacaoRepository observacaoRepository,
			ClienteStorage storageClient,
			Clock clock) {
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.fotoRepository = fotoRepository;
		this.historicoRepository = historicoRepository;
		this.encaminhamentoRepository = encaminhamentoRepository;
		this.observacaoRepository = observacaoRepository;
		this.storageClient = storageClient;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<AdminOcorrenciaListItem> list(AdminOcorrenciaFilter filter, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Ocorrencia> ocorrencias = ocorrenciaRepository.findAll(OcorrenciaSpecifications.from(filter),
				pageable);
		Map<UUID, Long> fotoCounts = fotoCounts(ocorrencias.getContent());

		return PaginaResponse.from(ocorrencias.map(ocorrencia -> toListItem(ocorrencia,
				fotoCounts.getOrDefault(ocorrencia.getId(), 0L))));
	}

	@Transactional(readOnly = true)
	public AdminOcorrenciaDetail detail(UUID id) {
		Ocorrencia ocorrencia = find(id);
		return toDetail(ocorrencia, fotoViews(ocorrencia));
	}

	@Transactional(readOnly = true)
	public List<AdminOcorrenciaDetail.FotoView> fotos(UUID id) {
		Ocorrencia ocorrencia = find(id);
		return fotoViews(ocorrencia);
	}

	@Transactional
	public AdminOcorrenciaDetail changeStatus(UUID id, StatusChangeRequest request, String adminId) {
		Ocorrencia ocorrencia = lock(id);
		OcorrenciaStatus from = ocorrencia.getStatus();
		OcorrenciaStatus to = request.status();
		if (from == to) {
			throw Excecoes.conflict("A ocorrência já está nesta situação.");
		}

		String observacao = normalize(request.observacao(), "observacao");
		Instant now = clock.instant();
		boolean reopening = from == OcorrenciaStatus.RESOLVIDA && to != OcorrenciaStatus.RESOLVIDA;

		if (to == OcorrenciaStatus.RESOLVIDA) {
			if (!NormalizadorTexto.hasText(observacao)) {
				throw Excecoes.unprocessable(
						"Informe a confirmação de que o local foi atendido para marcar como resolvida.");
			}
			ocorrencia.markResolved(now, adminId, observacao);
		}
		else if (reopening) {
			if (!NormalizadorTexto.hasText(observacao)) {
				throw Excecoes.unprocessable(
						"Informe o motivo para reabrir uma ocorrência já resolvida.");
			}
			ocorrencia.clearResolution();
			ocorrencia.setStatus(to);
		}
		else {
			ocorrencia.setStatus(to);
		}

		ocorrencia.touch(now);
		historicoRepository.saveAndFlush(OcorrenciaStatusHistorico.create(ocorrencia, from, to, adminId, observacao, now));
		return toDetail(ocorrencia, fotoViews(ocorrencia));
	}

	@Transactional
	public AdminOcorrenciaDetail changePrioridade(UUID id, PrioridadeChangeRequest request) {
		Ocorrencia ocorrencia = lock(id);
		String reason = normalize(request.reason(), "reason");
		ocorrencia.applyManualPrioridade(request.prioridade(), reason);
		ocorrencia.touch(clock.instant());
		return toDetail(ocorrencia, fotoViews(ocorrencia));
	}

	@Transactional
	public AdminOcorrenciaDetail changeVulneravelArea(UUID id, VulneravelAreaRequest request) {
		Ocorrencia ocorrencia = lock(id);
		ocorrencia.setInVulneravelArea(request.inVulneravelArea());
		if (!ocorrencia.isPrioridadeSetManually()) {
			PrioridadeResult result = PrioridadeCalculator.calculate(ocorrencia.getTipo(),
					ocorrencia.getVolumeEstimado(), ocorrencia.getAcessoColeta(),
					ocorrencia.getRiscoDrenagem(), Boolean.TRUE.equals(request.inVulneravelArea()));
			ocorrencia.applyPrioridade(result);
		}
		ocorrencia.touch(clock.instant());
		return toDetail(ocorrencia, fotoViews(ocorrencia));
	}

	@Transactional
	public AdminOcorrenciaDetail confirmLocalizacao(UUID id, LocalizacaoConfirmRequest request) {
		Ocorrencia ocorrencia = lock(id);
		ocorrencia.setLatitude(request.latitude().setScale(6, RoundingMode.HALF_UP));
		ocorrencia.setLongitude(request.longitude().setScale(6, RoundingMode.HALF_UP));
		ocorrencia.setPrecisaoMetros(request.precisaoMeters() == null ? null
				: request.precisaoMeters().setScale(2, RoundingMode.HALF_UP));
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.CONFIRMADA_PELA_EQUIPE);
		ocorrencia.touch(clock.instant());
		return toDetail(ocorrencia, fotoViews(ocorrencia));
	}

	@Transactional
	public AdminOcorrenciaDetail.ObservacaoView addObservacao(UUID id, ObservacaoCreateRequest request, String adminId) {
		Ocorrencia ocorrencia = find(id);
		Instant now = clock.instant();
		ocorrencia.touch(now);
		OcorrenciaInternalObservacao observacao = observacaoRepository.save(OcorrenciaInternalObservacao.create(ocorrencia, adminId,
				normalizeRequired(request.body(), "body"), now));
		return new AdminOcorrenciaDetail.ObservacaoView(observacao.getId(), observacao.getAutorId(), observacao.getCorpo(),
				observacao.getCreatedAt());
	}

	@Transactional
	public void deleteFoto(UUID ocorrenciaId, UUID fotoId, String adminId) {
		Ocorrencia ocorrencia = lock(ocorrenciaId);
		OcorrenciaFoto foto = fotoRepository.findByIdAndOcorrenciaId(fotoId, ocorrenciaId)
				.orElseThrow(() -> Excecoes.notFound("Foto não encontrada para esta ocorrência."));

		if (!storageClient.isConfigured()) {
			throw Excecoes.serviceUnavailable(
					"O armazenamento de fotos não está configurado. A foto não foi removida.");
		}
		try {
			storageClient.delete(foto.getCaminhoStorage());
		}
		catch (ExcecaoStorage exception) {
			throw Excecoes.serviceUnavailable(
					"Não foi possível remover a foto do armazenamento. Tente novamente.");
		}

		fotoRepository.delete(foto);
		Instant now = clock.instant();
		historicoRepository.save(OcorrenciaStatusHistorico.create(ocorrencia, ocorrencia.getStatus(),
				ocorrencia.getStatus(), adminId, "Uma foto foi removida pela equipe.", now));
		ocorrencia.touch(now);
	}

	private Map<UUID, Long> fotoCounts(List<Ocorrencia> ocorrencias) {
		if (ocorrencias.isEmpty()) {
			return Map.of();
		}
		List<UUID> ids = ocorrencias.stream().map(Ocorrencia::getId).toList();
		Map<UUID, Long> counts = new HashMap<>();
		for (Object[] row : fotoRepository.countByOcorrenciaIds(ids)) {
			counts.put((UUID) row[0], (Long) row[1]);
		}
		return counts;
	}

	private AdminOcorrenciaListItem toListItem(Ocorrencia ocorrencia, long fotoCount) {
		return new AdminOcorrenciaListItem(
				ocorrencia.getId(),
				ocorrencia.getNumero(),
				ocorrencia.getCreatedAt(),
				ocorrencia.getTipo(),
				ocorrencia.getVolumeEstimado(),
				ocorrencia.getAcessoColeta(),
				ocorrencia.getRiscoDrenagem(),
				ocorrencia.getStatus(),
				ocorrencia.getPrioridade(),
				ocorrencia.isPrioridadeSetManually(),
				ocorrencia.getTextoEndereco(),
				ocorrencia.getPontoReferencia(),
				ocorrencia.getLatitude() != null,
				fotoCount);
	}

	private AdminOcorrenciaDetail toDetail(Ocorrencia ocorrencia, List<AdminOcorrenciaDetail.FotoView> fotos) {
		List<AdminOcorrenciaDetail.HistoricoView> historico = historicoRepository
				.findByOcorrenciaIdOrderByAlteradoEmAsc(ocorrencia.getId()).stream()
				.map(item -> new AdminOcorrenciaDetail.HistoricoView(item.getStatusAnterior(), item.getStatusNovo(),
						item.getAlteradoPor(), item.getObservacao(), item.getAlteradoEm()))
				.toList();

		List<AdminOcorrenciaDetail.EncaminhamentoView> encaminhamentos = encaminhamentoRepository
				.findByOcorrenciaIdOrderByEncaminhadoEmDesc(ocorrencia.getId()).stream()
				.map(this::toEncaminhamentoView)
				.toList();

		List<AdminOcorrenciaDetail.ObservacaoView> observacaos = observacaoRepository
				.findByOcorrenciaIdOrderByCreatedAtAsc(ocorrencia.getId()).stream()
				.map(observacao -> new AdminOcorrenciaDetail.ObservacaoView(observacao.getId(), observacao.getAutorId(),
						observacao.getCorpo(), observacao.getCreatedAt()))
				.toList();

		List<AdminOcorrenciaDetail.PrioridadeReasonView> reasons = ocorrencia.getPrioridadeReasons().stream()
				.map(code -> {
					PrioridadeReason reason = PrioridadeReason.fromCode(code);
					return new AdminOcorrenciaDetail.PrioridadeReasonView(code,
							reason == null ? code : reason.label());
				})
				.toList();

		return new AdminOcorrenciaDetail(
				ocorrencia.getId(),
				ocorrencia.getNumero(),
				ocorrencia.getCreatedAt(),
				ocorrencia.getUpdatedAt(),
				ocorrencia.getTipo(),
				ocorrencia.getVolumeEstimado(),
				ocorrencia.getAcessoColeta(),
				ocorrencia.getRiscoDrenagem(),
				ocorrencia.getObservacoes(),
				ocorrencia.getLatitude(),
				ocorrencia.getLongitude(),
				ocorrencia.getPrecisaoMetros(),
				ocorrencia.getTextoEndereco(),
				ocorrencia.getPontoReferencia(),
				ocorrencia.getOrigemLocalizacao(),
				ocorrencia.getStatus(),
				ocorrencia.getPrioridade(),
				reasons,
				ocorrencia.isPrioridadeSetManually(),
				ocorrencia.getManualPrioridadeReason(),
				ocorrencia.getInVulneravelArea(),
				ocorrencia.getResolvedAt(),
				ocorrencia.getResolvedConfirmedBy(),
				ocorrencia.getResolutionObservacao(),
				historico,
				encaminhamentos,
				observacaos,
				fotos);
	}

	private AdminOcorrenciaDetail.EncaminhamentoView toEncaminhamentoView(OcorrenciaEncaminhamento encaminhamento) {
		return new AdminOcorrenciaDetail.EncaminhamentoView(encaminhamento.getId(), encaminhamento.getDestino(),
				encaminhamento.getCanal(), encaminhamento.getEncaminhadoEm(), encaminhamento.getNumeroProtocolo(),
				encaminhamento.getTextoSolicitacao(), encaminhamento.getEncaminhadoPor(), encaminhamento.getCreatedAt());
	}

	private List<AdminOcorrenciaDetail.FotoView> fotoViews(Ocorrencia ocorrencia) {
		List<AdminOcorrenciaDetail.FotoView> views = new ArrayList<>();
		for (OcorrenciaFoto foto : fotoRepository.findByOcorrenciaIdOrderByCreatedAtAsc(ocorrencia.getId())) {
			views.add(new AdminOcorrenciaDetail.FotoView(foto.getId(), signedUrlOrNull(foto),
					foto.getCreatedAt()));
		}
		return views;
	}

	private String signedUrlOrNull(OcorrenciaFoto foto) {
		try {
			return storageClient.signedUrl(foto.getCaminhoStorage());
		}
		catch (ExcecaoStorage exception) {
			log.warn("Não foi possível gerar link assinado de foto da ocorrência {}", foto.getOcorrencia().getNumero());
			return null;
		}
	}

	private Ocorrencia find(UUID id) {
		return ocorrenciaRepository.findById(id)
				.orElseThrow(() -> Excecoes.notFound("Ocorrência não encontrada."));
	}

	private Ocorrencia lock(UUID id) {
		return ocorrenciaRepository.findByIdForUpdate(id)
				.orElseThrow(() -> Excecoes.notFound("Ocorrência não encontrada."));
	}

	private String normalize(String value, String field) {
		if (NormalizadorTexto.hasControlCharacters(value)) {
			throw Excecoes.badRequest("Remova caracteres inválidos do texto.",
					List.of(new ErroValidacaoCampo(field, "Remova caracteres inválidos do texto.")));
		}
		return NormalizadorTexto.normalize(value);
	}

	private String normalizeRequired(String value, String field) {
		String normalized = normalize(value, field);
		if (!NormalizadorTexto.hasText(normalized)) {
			throw Excecoes.badRequest("Este campo não pode ficar em branco.",
					List.of(new ErroValidacaoCampo(field, "Este campo não pode ficar em branco.")));
		}
		return normalized;
	}
}
