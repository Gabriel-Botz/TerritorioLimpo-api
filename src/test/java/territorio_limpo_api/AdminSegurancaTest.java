package territorio_limpo_api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdminSegurancaTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	private RequestPostProcessor admin() {
		return jwt().jwt(token -> token.subject("admin-1")
				.claim("app_metadata", Map.of("app_role", "ADMIN")));
	}

	@Test
	@DisplayName("rota administrativa sem token responde 401")
	void rejectsWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/ocorrencias"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("token sem claim de admin responde 403")
	void rejectsWithoutAdminClaim() throws Exception {
		mockMvc.perform(get("/api/v1/admin/ocorrencias")
						.with(jwt().jwt(token -> token.subject("usuario-1"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("token com claim de admin acessa a listagem")
	void acceptsAdminClaim() throws Exception {
		mockMvc.perform(get("/api/v1/admin/ocorrencias").with(admin()))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("health é público")
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}
}
