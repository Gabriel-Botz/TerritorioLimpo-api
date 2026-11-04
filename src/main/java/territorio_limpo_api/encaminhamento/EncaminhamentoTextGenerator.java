package territorio_limpo_api.encaminhamento;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import territorio_limpo_api.common.NormalizadorTexto;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.Ocorrencia;

/**
 * Gera o texto de solicitação usado pela equipe ao encaminhar uma ocorrência.
 * O texto apenas descreve o registro; nunca afirma que um órgão recebeu,
 * agendou ou resolveu o atendimento.
 */
@Component
public class EncaminhamentoTextGenerator {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public String generate(Ocorrencia ocorrencia) {
		StringBuilder text = new StringBuilder();
		text.append("SOLICITAÇÃO DE COLETA — Território Limpo\n");
		text.append("Registro nº ").append(ocorrencia.getNumero()).append(" — registrado em ")
				.append(DATE_TIME.format(ocorrencia.getCriadoEm().atZone(SAO_PAULO))).append("\n\n");

		appendLine(text, "Tipo de resíduo", ocorrencia.getTipo().label());
		appendLine(text, "Volume estimado", ocorrencia.getVolumeEstimado().label());
		appendLine(text, "Acesso para caminhão", acessoLabel(ocorrencia.getAcessoColeta()));
		appendLine(text, "Bueiro obstruído ou água acumulada", ocorrencia.getRiscoDrenagem().label());

		text.append("\nLocalização:\n");
		appendLine(text, "Endereço", ocorrencia.getTextoEndereco());
		appendLine(text, "Ponto de referência", ocorrencia.getPontoReferencia());

		if (ocorrencia.getLatitude() != null && ocorrencia.getLongitude() != null) {
			String latitude = ocorrencia.getLatitude().toPlainString();
			String longitude = ocorrencia.getLongitude().toPlainString();
			appendLine(text, "Coordenadas", latitude + ", " + longitude + " ("
					+ ocorrencia.getOrigemLocalizacao().label() + ")");
			appendLine(text, "Mapa", "https://www.google.com/maps?q=" + latitude + "," + longitude);
		}

		if (NormalizadorTexto.hasText(ocorrencia.getObservacoes())) {
			text.append("\nObservações do morador: ").append(ocorrencia.getObservacoes()).append("\n");
		}

		text.append("\nEste texto foi gerado pelo sistema para apoiar o encaminhamento pela equipe. ")
				.append("O registro não garante atendimento nem prazo.\n");
		return text.toString();
	}

	private void appendLine(StringBuilder text, String label, String value) {
		if (NormalizadorTexto.hasText(value)) {
			text.append(label).append(": ").append(value).append("\n");
		}
	}

	private String acessoLabel(AcessoColeta acesso) {
		return switch (acesso) {
			case NORMAL -> "Normal";
			case DIFICIL -> "Difícil";
			case IMPOSSIVEL_CAMINHAO -> "Aparentemente impossível";
		};
	}
}
