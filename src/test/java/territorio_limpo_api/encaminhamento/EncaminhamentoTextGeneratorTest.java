package territorio_limpo_api.encaminhamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.OcorrenciaType;

class EncaminhamentoTextGeneratorTest {

	private final EncaminhamentoTextGenerator generator = new EncaminhamentoTextGenerator();

	private Ocorrencia ocorrencia() {
		Ocorrencia ocorrencia = Ocorrencia.create(UUID.randomUUID(), Instant.parse("2026-11-03T14:30:00Z"));
		ReflectionTestUtils.setField(ocorrencia, "numero", 42L);
		ocorrencia.setTipo(OcorrenciaType.ENTULHO);
		ocorrencia.setVolumeEstimado(VolumeEstimado.MEDIO);
		ocorrencia.setAcessoColeta(AcessoColeta.DIFICIL);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.NAO_SEI);
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		ocorrencia.setPontoReferencia("perto da escola");
		ocorrencia.setObservacoes("Restos de obra na calçada.");
		return ocorrencia;
	}

	@Test
	@DisplayName("gera texto com campos principais")
	void generatesText() {
		org.assertj.core.api.Assertions.assertThat(generator.generate(ocorrencia())).contains("COLETA");
	}

	@Test
	void includesCoordinatesAndMap() {
		Ocorrencia ocorrencia = ocorrencia();
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.GPS);
		ocorrencia.setLatitude(new BigDecimal("-22.505000"));
		ocorrencia.setLongitude(new BigDecimal("-43.178000"));
		org.assertj.core.api.Assertions.assertThat(generator.generate(ocorrencia)).contains("Coordenadas:");
	}

	@Test
	void omitsMissingValues() {
		Ocorrencia ocorrencia = ocorrencia();
		ocorrencia.setTextoEndereco(null);
		ocorrencia.setPontoReferencia(null);
		ocorrencia.setObservacoes(null);
		org.assertj.core.api.Assertions.assertThat(generator.generate(ocorrencia)).contains("SOLICITA");
	}
}
