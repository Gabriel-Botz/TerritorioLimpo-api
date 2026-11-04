package territorio_limpo_api.ocorrencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import territorio_limpo_api.common.Excecoes;
import territorio_limpo_api.common.ErroValidacaoCampo;
import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.historico.OcorrenciaStatusHistorico;
import territorio_limpo_api.historico.OcorrenciaStatusHistoricoRepository;
import territorio_limpo_api.ocorrencia.dto.OcorrenciaCreatedResponse;
import territorio_limpo_api.ocorrencia.dto.PublicOcorrenciaRequest;
import territorio_limpo_api.foto.ExcecaoFotoInvalida;
import territorio_limpo_api.foto.OcorrenciaFoto;
import territorio_limpo_api.foto.OcorrenciaFotoRepository;
import territorio_limpo_api.foto.FotoProcessor;
import territorio_limpo_api.foto.FotoProcessada;
import territorio_limpo_api.prioridade.PrioridadeCalculator;
import territorio_limpo_api.prioridade.PrioridadeResult;
import territorio_limpo_api.storage.ClienteStorage;
import territorio_limpo_api.storage.ExcecaoStorage;

@Service
public class PublicOcorrenciaService {

	private static final Logger log = LoggerFactory.getLogger(PublicOcorrenciaService.class);

	private static final String RECEIVED_MESSAGE =
			"Seu registro foi recebido e será analisado conforme a disponibilidade da equipe.";

	private final OcorrenciaRepository ocorrenciaRepository;
	private final OcorrenciaStatusHistoricoRepository historicoRepository;
	private final OcorrenciaFotoRepository fotoRepository;
	private final FotoProcessor fotoProcessor;
	private final ClienteStorage storageClient;
	private final EntityManager entityManager;
	private final Clock clock;

	public PublicOcorrenciaService(OcorrenciaRepository ocorrenciaRepository,
			OcorrenciaStatusHistoricoRepository historicoRepository,
			OcorrenciaFotoRepository fotoRepository,
			FotoProcessor fotoProcessor,
			ClienteStorage storageClient,
			EntityManager entityManager,
			Clock clock) {
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.historicoRepository = historicoRepository;
		this.fotoRepository = fotoRepository;
		this.fotoProcessor = fotoProcessor;
		this.storageClient = storageClient;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional
	public CreationResult create(PublicOcorrenciaRequest request, List<MultipartFile> fotos,
			UUID idempotencyKey) {
		if (idempotencyKey != null) {
			Optional<Ocorrencia> existing = ocorrenciaRepository.findByIdRequisicaoCliente(idempotencyKey);
			if (existing.isPresent()) {
				return new CreationResult(replay(existing.get()), true);
			}
		}

		NormalizedData data = normalizeAndValidate(request);
		List<FotoProcessada> processedFotos = processFotos(fotos);

		Instant now = clock.instant();
		UUID ocorrenciaId = UUID.randomUUID();
		Ocorrencia ocorrencia = buildOcorrencia(ocorrenciaId, request, data, idempotencyKey, now);

		// O id é atribuído pela aplicação, então o Spring Data trata a entidade como
		// existente e faz merge. É preciso usar a instância gerenciada devolvida.
		Ocorrencia stored = ocorrenciaRepository.saveAndFlush(ocorrencia);
		entityManager.refresh(stored);

		historicoRepository.save(OcorrenciaStatusHistorico.create(stored, null,
				OcorrenciaStatus.RECEBIDA, null, null, now));

		int received = processedFotos.size();
		int saved = 0;
		List<String> uploadedPaths = new ArrayList<>();
		try {
			for (FotoProcessada foto : processedFotos) {
				String path = ocorrenciaId + "/" + UUID.randomUUID() + ".jpg";
				try {
					storageClient.upload(path, foto.content());
					uploadedPaths.add(path);
					fotoRepository.save(OcorrenciaFoto.create(UUID.randomUUID(), stored, path,
							foto.contentType(), foto.content().length, clock.instant()));
					saved++;
				}
				catch (ExcecaoStorage exception) {
					log.warn("Falha ao armazenar foto da ocorrência {}", stored.getNumero());
				}
			}
		}
		catch (RuntimeException exception) {
			removeUploaded(uploadedPaths);
			throw exception;
		}

		int failed = received - saved;
		OcorrenciaCreatedResponse response = new OcorrenciaCreatedResponse(
				stored.getNumero(),
				stored.getCriadoEm(),
				data.localizacaoSummary(),
				stored.getSituacao(),
				new OcorrenciaCreatedResponse.FotoSummary(received, saved, failed),
				messageFor(failed));
		return new CreationResult(response, false);
	}

	public Optional<OcorrenciaCreatedResponse> findReplay(UUID idempotencyKey) {
		return ocorrenciaRepository.findByIdRequisicaoCliente(idempotencyKey).map(this::replay);
	}

	private Ocorrencia buildOcorrencia(UUID id, PublicOcorrenciaRequest request, NormalizedData data,
			UUID idempotencyKey, Instant now) {
		Ocorrencia ocorrencia = Ocorrencia.create(id, now);
		ocorrencia.setTipo(request.type());
		ocorrencia.setVolumeEstimado(request.estimatedVolume());
		ocorrencia.setAcessoColeta(request.collectionAcesso());
		ocorrencia.setRiscoDrenagem(request.drenagemRisk());
		ocorrencia.setObservacoes(data.observacaos());
		ocorrencia.setTextoEndereco(data.addressText());
		ocorrencia.setPontoReferencia(data.referenciaPoint());
		ocorrencia.setPrecisaoMetros(data.precisaoMeters());
		ocorrencia.setIdRequisicaoCliente(idempotencyKey);

		if (request.hasCoordinates()) {
			ocorrencia.setLatitude(coordinates(request.latitude()));
			ocorrencia.setLongitude(coordinates(request.longitude()));
			ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.GPS);
		}
		else {
			ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.MANUAL);
		}

		PrioridadeResult prioridade = PrioridadeCalculator.calculate(request.type(), request.estimatedVolume(),
				request.collectionAcesso(), request.drenagemRisk(), false);
		ocorrencia.applyPrioridade(prioridade);
		return ocorrencia;
	}

	private List<FotoProcessada> processFotos(List<MultipartFile> fotos) {
		if (fotos == null || fotos.isEmpty()) {
			return List.of();
		}
		if (fotos.size() > FotoProcessor.MAX_PHOTOS) {
			throw Excecoes.badRequest("Envie no máximo 3 fotos.",
					List.of(new ErroValidacaoCampo("fotos", "Envie no máximo 3 fotos.")));
		}

		List<FotoProcessada> processed = new ArrayList<>(fotos.size());
		for (MultipartFile foto : fotos) {
			try {
				processed.add(fotoProcessor.process(foto));
			}
			catch (ExcecaoFotoInvalida exception) {
				throw Excecoes.badRequest(exception.getMessage(),
						List.of(new ErroValidacaoCampo("fotos", exception.getMessage())));
			}
		}
		return processed;
	}

	private NormalizedData normalizeAndValidate(PublicOcorrenciaRequest request) {
		List<ErroValidacaoCampo> errors = new ArrayList<>();
		validateText("observacaos", request.observacaos(), errors);
		validateText("addressText", request.addressText(), errors);
		validateText("referenciaPoint", request.referenciaPoint(), errors);
		if (!errors.isEmpty()) {
			throw Excecoes.badRequest("Verifique os campos informados e tente novamente.", errors);
		}

		String observacaos = NormalizadorTexto.normalize(request.observacaos());
		String addressText = NormalizadorTexto.normalize(request.addressText());
		String referenciaPoint = NormalizadorTexto.normalize(request.referenciaPoint());

		return new NormalizedData(observacaos, addressText, referenciaPoint,
				request.precisaoMeters() == null ? null
						: request.precisaoMeters().setScale(2, RoundingMode.HALF_UP),
				localizacaoSummary(addressText, referenciaPoint));
	}

	private void validateText(String field, String value, List<ErroValidacaoCampo> errors) {
		if (NormalizadorTexto.hasControlCharacters(value)) {
			errors.add(new ErroValidacaoCampo(field, "Remova caracteres inválidos do texto."));
		}
	}

	private String localizacaoSummary(String addressText, String referenciaPoint) {
		if (NormalizadorTexto.hasText(addressText) && NormalizadorTexto.hasText(referenciaPoint)) {
			return addressText + " (" + referenciaPoint + ")";
		}
		if (NormalizadorTexto.hasText(addressText)) {
			return addressText;
		}
		if (NormalizadorTexto.hasText(referenciaPoint)) {
			return referenciaPoint;
		}
		return "Localização registrada por GPS";
	}

	private BigDecimal coordinates(BigDecimal value) {
		return value.setScale(6, RoundingMode.HALF_UP);
	}

	private OcorrenciaCreatedResponse replay(Ocorrencia ocorrencia) {
		int savedFotos = (int) fotoRepository.countByOcorrenciaId(ocorrencia.getId());
		return new OcorrenciaCreatedResponse(
				ocorrencia.getNumero(),
				ocorrencia.getCriadoEm(),
				localizacaoSummary(ocorrencia.getTextoEndereco(), ocorrencia.getPontoReferencia()),
				ocorrencia.getSituacao(),
				new OcorrenciaCreatedResponse.FotoSummary(savedFotos, savedFotos, 0),
				RECEIVED_MESSAGE);
	}

	private String messageFor(int failedFotos) {
		if (failedFotos == 0) {
			return RECEIVED_MESSAGE;
		}
		if (failedFotos == 1) {
			return "Seu registro foi recebido. Não foi possível salvar 1 foto, mas a ocorrência será analisada "
					+ "conforme a disponibilidade da equipe.";
		}
		return "Seu registro foi recebido. Não foi possível salvar " + failedFotos
				+ " fotos, mas a ocorrência será analisada conforme a disponibilidade da equipe.";
	}

	private void removeUploaded(List<String> paths) {
		for (String path : paths) {
			try {
				storageClient.delete(path);
			}
			catch (ExcecaoStorage exception) {
				log.warn("Falha ao remover foto compensada do armazenamento");
			}
		}
	}

	public record CreationResult(OcorrenciaCreatedResponse response, boolean replayed) {
	}

	private record NormalizedData(
			String observacaos,
			String addressText,
			String referenciaPoint,
			BigDecimal precisaoMeters,
			String localizacaoSummary) {
	}
}
