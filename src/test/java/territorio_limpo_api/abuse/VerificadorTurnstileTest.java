package territorio_limpo_api.abuse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import territorio_limpo_api.common.ExcecaoApi;
import territorio_limpo_api.config.TurnstileProperties;

class VerificadorTurnstileTest {

	private static final String VERIFY_URL = "https://turnstile.test/siteverify";

	private TurnstileProperties properties;
	private MockRestServiceServer server;
	private VerificadorTurnstile verifier;

	@BeforeEach
	void setUp() {
		properties = new TurnstileProperties();
		properties.setEnabled(true);
		properties.setSecret("segredo");
		properties.setVerifyUrl(VERIFY_URL);
		properties.setConnectTimeout(Duration.ofSeconds(1));
		properties.setReadTimeout(Duration.ofSeconds(1));

		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		verifier = new VerificadorTurnstile(properties, builder.build());
	}

	@Test
	@DisplayName("token válido é aceito sem enviar o IP")
	void acceptsValidToken() {
		server.expect(requestTo(VERIFY_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("secret=segredo&response=token-valido"))
				.andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

		verifier.verify("token-valido");

		server.verify();
	}

	@Test
	@DisplayName("token inválido gera erro 400")
	void rejectsInvalidToken() {
		server.expect(requestTo(VERIFY_URL))
				.andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> verifier.verify("token-invalido"))
				.isInstanceOf(ExcecaoApi.class)
				.satisfies(exception -> org.assertj.core.api.Assertions
						.assertThat(((ExcecaoApi) exception).getStatus().value()).isEqualTo(400));
	}

	@Test
	@DisplayName("token ausente gera erro 400")
	void rejectsMissingToken() {
		assertThatThrownBy(() -> verifier.verify(null))
				.isInstanceOf(ExcecaoApi.class)
				.satisfies(exception -> org.assertj.core.api.Assertions
						.assertThat(((ExcecaoApi) exception).getStatus().value()).isEqualTo(400));
	}

	@Test
	@DisplayName("falha do serviço de captcha gera erro 503 (fail-closed)")
	void failsClosedWhenServiceIsDown() {
		server.expect(requestTo(VERIFY_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> verifier.verify("token"))
				.isInstanceOf(ExcecaoApi.class)
				.satisfies(exception -> org.assertj.core.api.Assertions
						.assertThat(((ExcecaoApi) exception).getStatus().value()).isEqualTo(503));
	}

	@Test
	@DisplayName("verificação desligada não chama o serviço")
	void disabledSkipsVerification() {
		properties.setEnabled(false);

		verifier.verify(null);

		server.verify();
	}
}
