package territorio_limpo_api.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Falha rápido em produção quando falta configuração obrigatória.
 */
@Component
@Profile("prod")
public class VerificacaoConfiguracaoProducao implements InitializingBean {

	private final AuthProperties authProperties;
	private final StorageProperties storageProperties;
	private final CorsProperties corsProperties;

	public VerificacaoConfiguracaoProducao(AuthProperties authProperties, StorageProperties storageProperties,
			CorsProperties corsProperties) {
		this.authProperties = authProperties;
		this.storageProperties = storageProperties;
		this.corsProperties = corsProperties;
	}

	@Override
	public void afterPropertiesSet() {
		require(authProperties.hasJwks() || authProperties.hasSecret(),
				"Configure SUPABASE_JWKS_URI (recomendado) ou SUPABASE_JWT_SECRET.");
		require(storageProperties.isConfigured(),
				"Configure SUPABASE_URL, SUPABASE_SERVICE_KEY e SUPABASE_BUCKET.");
		require(!corsProperties.getAllowedOrigins().isEmpty(),
				"Configure CORS_ALLOWED_ORIGINS com pelo menos uma origem.");
		require(corsProperties.getAllowedOrigins().stream().noneMatch(origin -> origin.contains("*")),
				"CORS_ALLOWED_ORIGINS não pode usar curinga em produção.");
	}

	private void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException("Configuração de produção incompleta: " + message);
		}
	}
}
