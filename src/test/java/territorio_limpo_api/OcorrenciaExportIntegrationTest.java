package territorio_limpo_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import territorio_limpo_api.observacao.OcorrenciaInternalObservacao;
import territorio_limpo_api.observacao.OcorrenciaInternalObservacaoRepository;
import territorio_limpo_api.ocorrencia.AcessoColeta;
import territorio_limpo_api.ocorrencia.RiscoDrenagem;
import territorio_limpo_api.ocorrencia.VolumeEstimado;
import territorio_limpo_api.ocorrencia.OrigemLocalizacao;
import territorio_limpo_api.ocorrencia.Ocorrencia;
import territorio_limpo_api.ocorrencia.OcorrenciaRepository;
import territorio_limpo_api.ocorrencia.OcorrenciaType;
import territorio_limpo_api.prioridade.PrioridadeCalculator;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamento;
import territorio_limpo_api.encaminhamento.OcorrenciaEncaminhamentoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OcorrenciaExportIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OcorrenciaRepository ocorrenciaRepository;

	@Autowired
	private OcorrenciaEncaminhamentoRepository encaminhamentoRepository;

	@Autowired
	private OcorrenciaInternalObservacaoRepository observacaoRepository;

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
		ocorrencia.setTextoEndereco("Rua \"Exemplo\", 100");
		ocorrencia.setPontoReferencia("perto da escola");
		ocorrencia.setObservacoes("=SUM(1,1)\nsegunda linha");
		ocorrencia.applyPrioridade(PrioridadeCalculator.calculate(OcorrenciaType.LIXO_COMUM,
				VolumeEstimado.PEQUENO, AcessoColeta.NORMAL, RiscoDrenagem.NAO, false));
		return ocorrenciaRepository.saveAndFlush(ocorrencia);
	}

	@Test
	@DisplayName("CSV tem BOM, separador, CRLF, neutraliza fórmulas e omite notas internas")
	void exportsCsv() throws Exception {
		Ocorrencia ocorrencia = ocorrencia();
		encaminhamentoRepository.saveAndFlush(OcorrenciaEncaminhamento.create(ocorrencia, "CONDEP", "E-mail",
				Instant.now(), "PROT-1", "Texto", "admin-1", Instant.now()));
		observacaoRepository.saveAndFlush(OcorrenciaInternalObservacao.create(ocorrencia, "admin-1",
				"SEGREDO INTERNO DA EQUIPE", Instant.now()));

		MvcResult started = mockMvc.perform(get("/api/v1/admin/ocorrencias/export.csv").with(admin()))
				.andExpect(request().asyncStarted())
				.andReturn();

		MvcResult result = mockMvc.perform(asyncDispatch(started))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
						org.hamcrest.Matchers.containsString("ocorrencias-")))
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andReturn();

		byte[] body = result.getResponse().getContentAsByteArray();
		assertThat(body).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);

		String csv = new String(body, StandardCharsets.UTF_8);
		assertThat(csv).contains("Número;Data/hora do registro;Tipo;Volume");
		assertThat(csv).contains("\r\n");
		assertThat(csv).contains("'=SUM(1,1)");
		assertThat(csv).contains("\"Rua \"\"Exemplo\"\", 100\"");
		assertThat(csv).contains("PROT-1");
		assertThat(csv).doesNotContain("SEGREDO INTERNO DA EQUIPE");
	}
}
