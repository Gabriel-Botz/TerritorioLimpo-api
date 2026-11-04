package territorio_limpo_api.abuse;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import territorio_limpo_api.common.Excecoes;
import territorio_limpo_api.config.TurnstileProperties;

/**
 * Verificação opcional do Cloudflare Turnstile. O IP do morador nunca é
 * enviado ao serviço de captcha.
 */
@Component
public class VerificadorTurnstile {

	private final TurnstileProperties properties;
	private final RestClient restClient;

	@Autowired
	public VerificadorTurnstile(TurnstileProperties properties) {
		this(properties, configure(properties));
	}

	VerificadorTurnstile(TurnstileProperties properties, RestClient restClient) {
		this.properties = properties;
		this.restClient = restClient;
	}

	private static RestClient configure(TurnstileProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
		requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
		return RestClient.builder().requestFactory(requestFactory).build();
	}

	public void verify(String token) {
		if (!properties.isEnabled()) {
			return;
		}
		if (!StringUtils.hasText(token)) {
			throw Excecoes.badRequest("Confirme que você não é um robô e tente novamente.");
		}
		if (!StringUtils.hasText(properties.getSecret())) {
			throw Excecoes.serviceUnavailable(
					"A verificação de segurança não está disponível no momento. Tente novamente em instantes.");
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", properties.getSecret());
		form.add("response", token);

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restClient.post()
					.uri(properties.getVerifyUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);

			if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
				throw Excecoes.badRequest(
						"A verificação de segurança não foi concluída. Tente novamente.");
			}
		}
		catch (RestClientException exception) {
			throw Excecoes.serviceUnavailable(
					"Não foi possível concluir a verificação de segurança agora. Tente novamente em instantes.");
		}
	}
}
