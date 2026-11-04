package territorio_limpo_api.storage;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import territorio_limpo_api.config.StorageProperties;

/**
 * Cliente mínimo do Supabase Storage. O bucket é privado e só o backend usa a
 * chave de serviço; o frontend recebe URLs assinadas de curta duração.
 */
@Component
public class ClienteStorageSupabase implements ClienteStorage {

	private final StorageProperties properties;
	private final RestClient restClient;

	public ClienteStorageSupabase(StorageProperties properties) {
		this.properties = properties;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
		requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public boolean isConfigured() {
		return properties.isConfigured();
	}

	@Override
	public void upload(String path, byte[] content) {
		requireConfigured();
		try {
			restClient.post()
					.uri(objectUri(path))
					.headers(headers -> {
						headers.setBearerAuth(properties.getServiceKey());
						headers.set("x-upsert", "false");
						headers.setContentType(MediaType.IMAGE_JPEG);
					})
					.body(content)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException exception) {
			throw new ExcecaoStorage("Não foi possível armazenar a foto.", exception);
		}
	}

	@Override
	public void delete(String path) {
		requireConfigured();
		try {
			restClient.method(HttpMethod.DELETE)
					.uri(objectUri(path))
					.headers(headers -> {
						headers.setBearerAuth(properties.getServiceKey());
						headers.setContentType(MediaType.APPLICATION_JSON);
					})
					.body(Map.of("prefixes", List.of(path)))
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException exception) {
			throw new ExcecaoStorage("Não foi possível remover a foto.", exception);
		}
	}

	@Override
	public String signedUrl(String path) {
		requireConfigured();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restClient.post()
					.uri(URI.create(baseUrl() + "/storage/v1/object/sign/" + properties.getBucket() + "/" + path))
					.headers(headers -> {
						headers.setBearerAuth(properties.getServiceKey());
						headers.setContentType(MediaType.APPLICATION_JSON);
					})
					.body(Map.of("expiresIn", ttlSeconds()))
					.retrieve()
					.body(Map.class);

			Object signed = response == null ? null : response.get("signedURL");
			if (signed == null || !StringUtils.hasText(signed.toString())) {
				throw new ExcecaoStorage("O serviço de armazenamento não devolveu a URL da foto.");
			}
			return baseUrl() + "/storage/v1" + signed;
		}
		catch (RestClientException exception) {
			throw new ExcecaoStorage("Não foi possível gerar o link da foto.", exception);
		}
	}

	private URI objectUri(String path) {
		return URI.create(baseUrl() + "/storage/v1/object/" + properties.getBucket() + "/" + path);
	}

	private String baseUrl() {
		return properties.getSupabaseUrl().replaceAll("/+$", "");
	}

	private int ttlSeconds() {
		Duration ttl = properties.getSignedUrlTtl();
		return Math.max(60, (int) ttl.toSeconds());
	}

	private void requireConfigured() {
		if (!properties.isConfigured()) {
			throw new ExcecaoStorage("O armazenamento de fotos não está configurado neste ambiente.");
		}
	}
}
