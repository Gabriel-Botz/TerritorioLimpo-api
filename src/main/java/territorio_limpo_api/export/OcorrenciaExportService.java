package territorio_limpo_api.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.ocorrencia.AdminOcorrenciaFilter;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaSpecifications;
import territorio_limpo_api.foto.OcorrenciaFotoRepository;
import territorio_limpo_api.prioridade.PrioridadeReason;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamentoRepository;

/**
 * Exporta ocorrências em CSV compatível com Excel em português: UTF-8 com
 * BOM, separador ponto e vírgula e quebras CRLF. A escrita é feita em lotes
 * de 500 registros, sem carregar todas as ocorrências em memória. Observações
 * internas da equipe nunca são exportadas.
 */
@Service
public class OcorrenciaExportService {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private static final String[] HEADERS = {
			"Número",
			"Data/hora do registro",
			"Tipo",
			"Volume",
			"Acesso para caminhão",
			"Risco de drenagem",
			"Situação",
			"Prioridade",
			"Motivos da prioridade",
			"Origem da localização",
			"Latitude",
			"Longitude",
			"Endereço",
			"Ponto de referência",
			"Observações do morador",
			"Nº de fotos",
			"Nº de encaminhamentos",
			"Último protocolo",
			"Data de resolução"
	};

	private final OcorrenciaRepository ocorrenciaRepository;
	private final OcorrenciaFotoRepository fotoRepository;
	private final OcorrenciaEncaminhamentoRepository encaminhamentoRepository;

	public OcorrenciaExportService(OcorrenciaRepository ocorrenciaRepository,
			OcorrenciaFotoRepository fotoRepository,
			OcorrenciaEncaminhamentoRepository encaminhamentoRepository) {
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.fotoRepository = fotoRepository;
		this.encaminhamentoRepository = encaminhamentoRepository;
	}

	@Transactional(readOnly = true)
	public void writeCsv(AdminOcorrenciaFilter filter, OutputStream outputStream) throws IOException {
		Map<UUID, Long> fotoCounts = toCountMap(fotoRepository.countGroupedByOcorrencia());
		Map<UUID, Long> encaminhamentoCounts = toCountMap(encaminhamentoRepository.countGroupedByOcorrencia());
		Map<UUID, String> lastProtocols = lastProtocols();

		outputStream.write(UTF8_BOM);
		Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
				.setDelimiter(';')
				.setRecordSeparator("\r\n")
				.setHeader(HEADERS)
				.build())) {
			Specification<Ocorrencia> specification = OcorrenciaSpecifications.from(filter);
			int pageNumber = 0;
			Page<Ocorrencia> page;
			do {
				page = ocorrenciaRepository.findAll(specification,
						PageRequest.of(pageNumber, 500, Sort.by(Sort.Direction.ASC, "criadoEm")));
				for (Ocorrencia ocorrencia : page.getContent()) {
					print(printer, ocorrencia,
							fotoCounts.getOrDefault(ocorrencia.getId(), 0L),
							encaminhamentoCounts.getOrDefault(ocorrencia.getId(), 0L),
							lastProtocols.get(ocorrencia.getId()));
				}
				pageNumber++;
			}
			while (page.hasNext());
		}
	}

	private void print(CSVPrinter printer, Ocorrencia ocorrencia, long fotoCount, long encaminhamentoCount,
			String lastProtocol) {
		try {
			printer.printRecord(
					ocorrencia.getNumero(),
					DATE_TIME.format(ocorrencia.getCriadoEm().atZone(SAO_PAULO)),
					TextoCsv.neutralize(ocorrencia.getTipo().label()),
					TextoCsv.neutralize(ocorrencia.getVolumeEstimado().label()),
					TextoCsv.neutralize(ocorrencia.getAcessoColeta().label()),
					TextoCsv.neutralize(ocorrencia.getRiscoDrenagem().label()),
					TextoCsv.neutralize(ocorrencia.getSituacao().label()),
					TextoCsv.neutralize(ocorrencia.getPrioridade().label()),
					TextoCsv.neutralize(motivosPrioridade(ocorrencia)),
					TextoCsv.neutralize(ocorrencia.getOrigemLocalizacao().label()),
					ocorrencia.getLatitude() == null ? "" : ocorrencia.getLatitude().toPlainString(),
					ocorrencia.getLongitude() == null ? "" : ocorrencia.getLongitude().toPlainString(),
					TextoCsv.neutralize(ocorrencia.getTextoEndereco()),
					TextoCsv.neutralize(ocorrencia.getPontoReferencia()),
					TextoCsv.neutralize(ocorrencia.getObservacoes()),
					fotoCount,
					encaminhamentoCount,
					TextoCsv.neutralize(lastProtocol),
					ocorrencia.getResolvidaEm() == null ? ""
							: DATE_TIME.format(ocorrencia.getResolvidaEm().atZone(SAO_PAULO)));
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private String motivosPrioridade(Ocorrencia ocorrencia) {
		List<String> labels = ocorrencia.getMotivosPrioridade().stream()
				.map(PrioridadeReason::fromCode)
				.filter(reason -> reason != null)
				.map(PrioridadeReason::label)
				.toList();
		if (labels.isEmpty() && NormalizadorTexto.hasText(ocorrencia.getMotivoPrioridadeManual())) {
			return ocorrencia.getMotivoPrioridadeManual();
		}
		return String.join("; ", labels);
	}

	private Map<UUID, Long> toCountMap(List<Object[]> rows) {
		Map<UUID, Long> counts = new HashMap<>();
		for (Object[] row : rows) {
			counts.put((UUID) row[0], (Long) row[1]);
		}
		return counts;
	}

	private Map<UUID, String> lastProtocols() {
		Map<UUID, String> protocols = new HashMap<>();
		for (Object[] row : encaminhamentoRepository.protocolHistoricoOrderedByEncaminhadoEm()) {
			String protocol = (String) row[1];
			if (NormalizadorTexto.hasText(protocol)) {
				protocols.put((UUID) row[0], protocol);
			}
		}
		return protocols;
	}
}
