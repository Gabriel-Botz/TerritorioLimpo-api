package territorio_limpo_api.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SegurancaConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			@Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> {
					authorize.requestMatchers("/api/v1/public/**", "/actuator/health").permitAll();
					if (docsEnabled) {
						authorize.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
								.permitAll();
					}
					authorize.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
					authorize.anyRequest().denyAll();
				})
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint((request, response, exception) ->
								writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
										"É necessário estar autenticado para acessar este recurso."))
						.accessDeniedHandler((request, response, exception) ->
								writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
										"Seu usuário não tem permissão para acessar este recurso.")))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
										"É necessário estar autenticado para acessar este recurso."))
						.accessDeniedHandler((request, response, exception) ->
								writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
										"Seu usuário não tem permissão para acessar este recurso.")));
		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter(AuthProperties properties) {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Object role = roleFromAppMetadata(jwt, properties.getAdminRoleClaim());
			if (role != null && properties.getAdminRoleValue().equals(role.toString())) {
				return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
			}
			return List.of();
		});
		return converter;
	}

	private Object roleFromAppMetadata(Jwt jwt, String claimName) {
		Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
		return appMetadata == null ? null : appMetadata.get(claimName);
	}

	@Bean
	public JwtDecoder jwtDecoder(AuthProperties properties) {
		if (properties.hasJwks()) {
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwksUri()).build();
			applyIssuer(decoder, properties);
			return decoder;
		}
		if (properties.hasSecret()) {
			SecretKey key = new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8),
					"HmacSHA256");
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
					.macAlgorithm(MacAlgorithm.HS256)
					.build();
			applyIssuer(decoder, properties);
			return decoder;
		}
		return token -> {
			throw new JwtException("Autenticação não configurada neste ambiente.");
		};
	}

	private void applyIssuer(NimbusJwtDecoder decoder, AuthProperties properties) {
		if (StringUtils.hasText(properties.getIssuer())) {
			decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
		}
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.getAllowedOrigins());
		configuration.setAllowedMethods(List.of(
				HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PATCH.name(),
				HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
		configuration.setAllowedHeaders(List.of(
				"Authorization", "Content-Type", "Idempotency-Key", "X-Turnstile-Token"));
		configuration.setExposedHeaders(List.of("Retry-After", "Content-Disposition"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
			throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("""
				{"type":"urn:territorio-limpo:error:security","title":"%s","status":%d,"detail":"%s"}"""
				.formatted(title, status, detail));
	}
}
