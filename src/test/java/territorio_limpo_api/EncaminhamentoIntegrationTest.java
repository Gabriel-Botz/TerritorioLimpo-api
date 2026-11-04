package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaStatus;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.prioridade.PrioridadeCalculator;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamentoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EncaminhamentoIntegrationTest {

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

	private Ocorrencia ocorrencia() {
		Ocorrencia ocorrencia = Ocorrencia.create(UUID.randomUUID(), Instant.now());
		ocorrencia.setTipo(OcorrenciaType.LIXO_COMUM);
		ocorrencia.setVolumeEstimado(VolumeEstimado.PEQUENO);
		ocorrencia.setAcessoColeta(AcessoColeta.NORMAL);
		ocorrencia.setRiscoDrenagem(RiscoDrenagem.NAO);
		ocorrencia.setOrigemLocalizacao(OrigemLocalizacao.MANUAL);
		ocorrencia.setTextoEndereco("Rua Exemplo, 100");
		ocorrencia.setPontoReferencia("perto da escola");
		ocorrencia.setObservacoes("Lixo acumulado há uma semana.");
		ocorrencia.applyPrioridade(PrioridadeCalculator.calculate(OcorrenciaType.LIXO_COMUM,
				VolumeEstimado.PEQUENO, AcessoColeta.NORMAL, RiscoDrenagem.NAO, false));
		return ocorrenciaRepository.saveAndFlush(ocorrencia);
	}

	@Test
	@DisplayName("devolve texto de encaminhamento em text/plain")
	void returnsEncaminhamentoText() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		mockMvc.perform(get("/api/v1/admin/ocorrencias/{id}/encaminhamento-text", ocorrencia.getId()).with(admin()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/plain"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"SOLICITAÇÃO DE COLETA — Território Limpo")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Endereço: Rua Exemplo, 100")));
	}

	@Test
	@DisplayName("registra encaminhamento com texto gerado e não altera o status")
	void createsEncaminhamentoWithoutChangingStatus() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		mockMvc.perform(post("/api/v1/admin/ocorrencias/{id}/encaminhamentos", ocorrencia.getId())
						.with(admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"destination":"CONDEP","channel":"E-mail institucional"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.destination").value("CONDEP"))
				.andExpect(jsonPath("$.channel").value("E-mail institucional"))
				.andExpect(jsonPath("$.referredBy").value("admin-1"))
				.andExpect(jsonPath("$.requestText").value(org.hamcrest.Matchers.containsString(
						"SOLICITAÇÃO DE COLETA")));

		Ocorrencia reloaded = ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow();
		assertThat(reloaded.getSituacao()).isEqualTo(OcorrenciaStatus.RECEBIDA);
		assertThat(encaminhamentoRepository.findByOcorrenciaIdOrderByEncaminhadoEmDesc(ocorrencia.getId())).hasSize(1);
	}

	@Test
	@DisplayName("usa o texto informado e guarda protocolo quando enviados")
	void usesProvidedTextAndProtocol() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();

		mockMvc.perform(post("/api/v1/admin/ocorrencias/{id}/encaminhamentos", ocorrencia.getId())
						.with(admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "destination":"Secretaria de Meio Ambiente",
								  "channel":"Protocolo online",
								  "protocolNumber":"PROT-2026-001",
								  "requestText":"Texto personalizado pela equipe."
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.protocolNumber").value("PROT-2026-001"))
				.andExpect(jsonPath("$.requestText").value("Texto personalizado pela equipe."));
	}
}
