package territorio_limpo_api.prioridade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;

class PrioridadeCalculatorTest {

	private static PrioridadeResult calculate(OcorrenciaType type, VolumeEstimado volume,
			AcessoColeta acesso, RiscoDrenagem drenagem) {
		return PrioridadeCalculator.calculate(type, volume, acesso, drenagem, false);
	}

	private static PrioridadeResult calculate(OcorrenciaType type, VolumeEstimado volume,
			AcessoColeta acesso, RiscoDrenagem drenagem, boolean vulneravelArea) {
		return PrioridadeCalculator.calculate(type, volume, acesso, drenagem, vulneravelArea);
	}

	@Test
	@DisplayName("sem nenhum critério de risco resulta em prioridade baixa")
	void noCriteriaIsLow() {
		PrioridadeResult result = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO);

		assertThat(result.score()).isZero();
		assertThat(result.level()).isEqualTo(NivelPrioridade.BAIXA);
		assertThat(result.reasons()).isEmpty();
	}

	@Test
	@DisplayName("risco de drenagem confirmado soma 3 pontos e motivo")
	void drenagemRiskYes() {
		PrioridadeResult result = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.NORMAL, RiscoDrenagem.SIM);

		assertThat(result.score()).isEqualTo(3);
		assertThat(result.level()).isEqualTo(NivelPrioridade.MEDIA);
		assertThat(result.reasons()).containsExactly(PrioridadeReason.DRENAGEM_COM_RISCO);
	}

	@Test
	@DisplayName("risco de drenagem incerto soma 1 ponto e motivo")
	void drenagemRiskUnknown() {
		PrioridadeResult result = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO_SEI);

		assertThat(result.score()).isEqualTo(1);
		assertThat(result.level()).isEqualTo(NivelPrioridade.BAIXA);
		assertThat(result.reasons()).containsExactly(PrioridadeReason.DRENAGEM_INCERTA);
	}

	@Test
	@DisplayName("tipo obstrução de drenagem soma 2 pontos")
	void obstructionType() {
		PrioridadeResult result = calculate(OcorrenciaType.DRENAGEM_OBSTRUIDA, VolumeEstimado.PEQUENO,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO);

		assertThat(result.score()).isEqualTo(2);
		assertThat(result.reasons()).containsExactly(PrioridadeReason.POSSIVEL_OBSTRUCAO);
	}

	@Test
	@DisplayName("volume grande soma 2 pontos e volume médio soma 1")
	void volumeWeights() {
		PrioridadeResult large = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.GRANDE,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO);
		PrioridadeResult medium = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.MEDIO,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO);

		assertThat(large.score()).isEqualTo(2);
		assertThat(large.reasons()).containsExactly(PrioridadeReason.VOLUME_GRANDE);
		assertThat(medium.score()).isEqualTo(1);
		assertThat(medium.reasons()).containsExactly(PrioridadeReason.VOLUME_MEDIO);
	}

	@Test
	@DisplayName("acesso difícil soma 1 e acesso impossível soma 2")
	void acessoWeights() {
		PrioridadeResult difficult = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.DIFICIL, RiscoDrenagem.NAO);
		PrioridadeResult impossible = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.IMPOSSIVEL_CAMINHAO, RiscoDrenagem.NAO);

		assertThat(difficult.score()).isEqualTo(1);
		assertThat(difficult.reasons()).containsExactly(PrioridadeReason.ACESSO_DIFICIL);
		assertThat(impossible.score()).isEqualTo(2);
		assertThat(impossible.reasons()).containsExactly(PrioridadeReason.ACESSO_IMPOSSIVEL_CAMINHAO);
	}

	@Test
	@DisplayName("área vulnerável verificada soma 2 pontos")
	void vulneravelArea() {
		PrioridadeResult result = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.PEQUENO,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO, true);

		assertThat(result.score()).isEqualTo(2);
		assertThat(result.reasons()).containsExactly(PrioridadeReason.AREA_VULNERAVEL_VERIFICADA);
	}

	@Test
	@DisplayName("limite inferior de prioridade média é 3 pontos")
	void mediumThreshold() {
		assertThat(PrioridadeCalculator.levelFor(2)).isEqualTo(NivelPrioridade.BAIXA);
		assertThat(PrioridadeCalculator.levelFor(3)).isEqualTo(NivelPrioridade.MEDIA);
		assertThat(PrioridadeCalculator.levelFor(4)).isEqualTo(NivelPrioridade.MEDIA);
	}

	@Test
	@DisplayName("limite inferior de prioridade alta é 5 pontos")
	void highThreshold() {
		assertThat(PrioridadeCalculator.levelFor(5)).isEqualTo(NivelPrioridade.ALTA);
		assertThat(PrioridadeCalculator.levelFor(10)).isEqualTo(NivelPrioridade.ALTA);
	}

	@Test
	@DisplayName("combinação de risco, volume e acesso gera prioridade alta")
	void combinedCriteriaIsHigh() {
		PrioridadeResult result = calculate(OcorrenciaType.DRENAGEM_OBSTRUIDA, VolumeEstimado.GRANDE,
				AcessoColeta.IMPOSSIVEL_CAMINHAO, RiscoDrenagem.SIM);

		assertThat(result.score()).isEqualTo(9);
		assertThat(result.level()).isEqualTo(NivelPrioridade.ALTA);
		assertThat(result.reasons()).containsExactly(
				PrioridadeReason.DRENAGEM_COM_RISCO,
				PrioridadeReason.POSSIVEL_OBSTRUCAO,
				PrioridadeReason.VOLUME_GRANDE,
				PrioridadeReason.ACESSO_IMPOSSIVEL_CAMINHAO);
	}

	@Test
	@DisplayName("todos os critérios somam o máximo previsto")
	void allCriteria() {
		PrioridadeResult result = calculate(OcorrenciaType.DRENAGEM_OBSTRUIDA, VolumeEstimado.GRANDE,
				AcessoColeta.IMPOSSIVEL_CAMINHAO, RiscoDrenagem.SIM, true);

		assertThat(result.score()).isEqualTo(11);
		assertThat(result.level()).isEqualTo(NivelPrioridade.ALTA);
		assertThat(result.reasons()).containsExactly(
				PrioridadeReason.DRENAGEM_COM_RISCO,
				PrioridadeReason.POSSIVEL_OBSTRUCAO,
				PrioridadeReason.VOLUME_GRANDE,
				PrioridadeReason.ACESSO_IMPOSSIVEL_CAMINHAO,
				PrioridadeReason.AREA_VULNERAVEL_VERIFICADA);
	}

	@Test
	@DisplayName("lista de motivos é imutável")
	void reasonsAreImmutable() {
		PrioridadeResult result = calculate(OcorrenciaType.LIXO_COMUM, VolumeEstimado.GRANDE,
				AcessoColeta.NORMAL, RiscoDrenagem.NAO);

		assertThat(result.reasons()).isUnmodifiable();
		assertThat(result.reasons()).isEqualTo(List.of(PrioridadeReason.VOLUME_GRANDE));
	}
}
