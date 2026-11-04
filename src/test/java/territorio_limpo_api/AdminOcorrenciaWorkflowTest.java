package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import territorio_limpo_api.historico.OcorrenciaStatusHistoricoRepository;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.ocorrencia.NivelPrioridade;
import territorio_limpo_api.prioridade.PrioridadeCalculator;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdminOcorrenciaWorkflowTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OcorrenciaRepository ocorrenciaRepository;

	@Autowired
	private OcorrenciaStatusHistoricoRepository historicoRepository;

	private RequestPostProcessor admin() {
		return jwt().jwt(token -> token.subject("admin-1")
				.claim("app_metadata", Map.of("app_role", "ADMIN")));
	}

	private Ocorrencia ocorrencia() {
		Ocorrencia ocorrencia = Ocorrencia.create(UUID.randomUUID(), Instant.now());
		ocorrencia.setTipo(OcorrenciaType.LIXO_COMUM);
		ocorrencia.setVolumeEstimado(VolumeEstimado.PEQUENO);
		ocorrencia.setAcessoColeta(AcessoColeta.NORMAL);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.NAO);
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		ocorrencia.applyPrioridade(PrioridadeCalculator.calculate(OcorrenciaType.LIXO_COMUM,
				VolumeEstimado.PEQUENO, AcessoColeta.NORMAL, RiscoDrenagem.NAO, false));
		return ocorrenciaRepository.saveAndFlush(ocorrencia);
	}

	private void changeStatus(Ocorrencia ocorrencia, String body, int expectedStatus) throws Exception {
		mockMvc.perform(patch("/api/v1/admin/ocorrencias/{id}/status", ocorrencia.getId())
						.with(admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().is(expectedStatus));
	}

	@Test
	@DisplayName("transição comum grava histórico")
	void commonTransitionWritesHistorico() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		changeStatus(ocorrencia, """
				{"status":"EM_ANALISE","observacao":"Triagem inicial."}
				""", 200);

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(OcorrenciaStatus.EM_ANALISE);
		assertThat(historicoRepository.findByOcorrenciaIdOrderByAlteradoEmAsc(ocorrencia.getId()))
				.singleElement()
				.satisfies(historico -> {
					assertThat(historico.getStatusAnterior()).isEqualTo(OcorrenciaStatus.RECEBIDA);
					assertThat(historico.getStatusNovo()).isEqualTo(OcorrenciaStatus.EM_ANALISE);
					assertThat(historico.getAlteradoPor()).isEqualTo("admin-1");
				});
	}

	@Test
	@DisplayName("mudar para o mesmo status responde 409")
	void sameStatusIsConflict() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		changeStatus(ocorrencia, """
				{"status":"RECEBIDA"}
				""", 409);
	}

	@Test
	@DisplayName("resolver sem confirmação responde 422")
	void resolvedRequiresObservacao() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		changeStatus(ocorrencia, """
				{"status":"RESOLVIDA"}
				""", 422);
	}

	@Test
	@DisplayName("resolver com confirmação preenche campos de resolução")
	void resolvedFillsResolution() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		changeStatus(ocorrencia, """
				{"status":"RESOLVIDA","observacao":"Coleta confirmada pela equipe no local."}
				""", 200);

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(OcorrenciaStatus.RESOLVIDA);
		assertThat(reloaded.getResolvedAt()).isNotNull();
		assertThat(reloaded.getResolvedConfirmedBy()).isEqualTo("admin-1");
		assertThat(reloaded.getResolutionObservacao()).isNotBlank();
	}

	@Test
	@DisplayName("reabrir exige motivo e limpa campos de resolução")
	void reopeningClearsResolution() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();
		changeStatus(ocorrencia, """
				{"status":"RESOLVIDA","observacao":"Coleta confirmada."}
				""", 200);

		changeStatus(ocorrencia, """
				{"status":"EM_ANALISE"}
				""", 422);

		changeStatus(ocorrencia, """
				{"status":"EM_ANALISE","observacao":"O local voltou a apresentar descarte."}
				""", 200);

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(OcorrenciaStatus.EM_ANALISE);
		assertThat(reloaded.getResolvedAt()).isNull();
		assertThat(reloaded.getResolvedConfirmedBy()).isNull();
		assertThat(reloaded.getResolutionObservacao()).isNull();
	}

	@Test
	@DisplayName("ajuste manual de prioridade marca a ocorrência como manual")
	void manualPrioridade() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		mockMvc.perform(patch("/api/v1/admin/ocorrencias/{id}/prioridade", ocorrencia.getId())
						.with(admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"prioridade":"ALTA","reason":"Risco observado pela equipe."}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prioridade").value("ALTA"))
				.andExpect(jsonPath("$.prioridadeSetManually").value(true));

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getPrioridade()).isEqualTo(NivelPrioridade.ALTA);
		assertThat(reloaded.isPrioridadeSetManually()).isTrue();
		assertThat(reloaded.getManualPrioridadeReason()).isEqualTo("Risco observado pela equipe.");
	}

	@Test
	@DisplayName("área vulnerável recalcula prioridade quando não foi ajustada manualmente")
	void vulneravelAreaRecalculatesPrioridade() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		mockMvc.perform(patch("/api/v1/admin/ocorrencias/{id}/vulneravel-area", ocorrencia.getId())
						.with(admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"inVulneravelArea":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prioridade").value("MEDIA"));

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getInVulneravelArea()).isTrue();
		assertThat(reloaded.getPrioridade()).isEqualTo(NivelPrioridade.MEDIA);
		assertThat(reloaded.getPrioridadeReasons()).contains("AREA_VULNERAVEL_VERIFICADA");
	}
}
