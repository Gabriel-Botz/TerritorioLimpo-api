package territorio_limpo_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"app.rate-limit.enabled=true",
		"app.rate-limit.per-ip-limit=1",
		"app.rate-limit.global-limit=100"
})
@Testcontainers(disabledWithoutDocker = true)
class PublicOcorrenciaRateLimitTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	private MockMultipartFile dataPart() {
		String json = """
				{
				  "type": "LIXO_COMUM",
				  "estimatedVolume": "PEQUENO",
				  "collectionAcesso": "NORMAL",
				  "drenagemRisk": "NAO",
				  "addressText": "Rua Exemplo, 100"
				}
				""";
		return new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
				json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("responde 429 com Retry-After após exceder o limite por IP")
	void blocksAfterLimit() throws Exception {
		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart())
						.header("X-Forwarded-For", "203.0.113.50"))
				.andExpect(status().isCreated());

		mockMvc.perform(multipart("/api/v1/public/ocorrencias")
						.file(dataPart())
						.header("X-Forwarded-For", "203.0.113.50"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}
}
