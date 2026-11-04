package territorio_limpo_api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.prioridade.PrioridadeCalculator;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamento;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamentoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class IndicadorIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OcorrenciaRepository ocorrenciaRepository;

	@Autowired
	private OcorrenciaEncaminhamentoRepository encaminhamentoRepository;

	private RequestPostProcessor admin() {
		return jwt().jwt(token -> token.subject("admin-1")
				.claim("app_metadata", Map.of("app_role", "ADMIN")));
	}

	private Ocorrencia ocorrencia(OcorrenciaStatus status, boolean highPrioridade) {
		Ocorrencia ocorrencia = Ocorrencia.create(UUID.randomUUID(), Instant.now());
		ocorrencia.setTipo(OcorrenciaType.LIXO_COMUM);
		ocorrencia.setVolumeEstimado(VolumeEstimado.PEQUENO);
		ocorrencia.setAcessoColeta(AcessoColeta.NORMAL);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.NAO);
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		if (highPrioridade) {
			ocorrencia.setTipo(OcorrenciaType.DRENAGEM_OBSTRUIDA);
			ocorrencia.setVolumeEstimado(VolumeEstimado.GRANDE);
			ocorrencia.setAcessoColeta(AcessoColeta.IMPOSSIVEL_CAMINHAO);
			ocorrencia.setRiscoDrenagem(RiscoDrenagem.SIM);
		}
		ocorrencia.applyPrioridade(PrioridadeCalculator.calculate(ocorrencia.getTipo(),
				ocorrencia.getVolumeEstimado(), ocorrencia.getAcessoColeta(),
				ocorrencia.getRiscoDrenagem(), false));
		if (status == OcorrenciaStatus.RESOLVIDA) {
			ocorrencia.markResolved(Instant.now(), "admin-1", "Coleta confirmada.");
		}
		else {
			ocorrencia.setSituacao(status);
		}
		return ocorrenciaRepository.saveAndFlush(ocorrencia);
	}

	private void encaminhamento(Ocorrencia ocorrencia, String protocol) {
		encaminhamentoRepository.saveAndFlush(OcorrenciaEncaminhamento.create(ocorrencia, "CONDEP", "E-mail",
				Instant.now(), protocol, "Texto", "admin-1", Instant.now()));
	}

	@Test
	@DisplayName("indicadores refletem a massa de dados conhecida")
	void indicadoresMatchKnownData() throws Exception {
		ocorrencia(OcorrenciaStatus.RECEBIDA, true);
		ocorrencia(OcorrenciaStatus.EM_ANALISE, false);
		Ocorrencia forwarded = ocorrencia(OcorrenciaStatus.ENCAMINHADA, false);
		Ocorrencia awaiting = ocorrencia(OcorrenciaStatus.AGUARDANDO_ATENDIMENTO, false);
		ocorrencia(OcorrenciaStatus.RESOLVIDA, false);
		ocorrencia(OcorrenciaStatus.NAO_SOLUCIONADA, false);

		encaminhamento(forwarded, "PROT-1");
		encaminhamento(awaiting, null);

		mockMvc.perform(get("/api/v1/admin/indicadores").with(admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalOcorrencias").value(6))
				.andExpect(jsonPath("$.received").value(1))
				.andExpect(jsonPath("$.inAnalysis").value(1))
				.andExpect(jsonPath("$.pendingAnalysis").value(2))
				.andExpect(jsonPath("$.forwarded").value(1))
				.andExpect(jsonPath("$.awaitingService").value(1))
				.andExpect(jsonPath("$.forwardedOrAwaiting").value(2))
				.andExpect(jsonPath("$.resolved").value(1))
				.andExpect(jsonPath("$.unresolved").value(1))
				.andExpect(jsonPath("$.highPrioridadePending").value(1))
				.andExpect(jsonPath("$.totalEncaminhamentos").value(2))
				.andExpect(jsonPath("$.encaminhamentosWithProtocol").value(1))
				.andExpect(jsonPath("$.ocorrenciasWithEncaminhamento").value(2));
	}
}
