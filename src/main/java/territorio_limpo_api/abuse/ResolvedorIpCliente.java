package territorio_limpo_api.abuse;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import territorio_limpo_api.config.RateLimitProperties;

@Component
public class ResolvedorIpCliente {

	private final RateLimitProperties properties;

	public ResolvedorIpCliente(RateLimitProperties properties) {
		this.properties = properties;
	}

	/**
	 * Lê o IP apenas do cabeçalho de proxy confiável configurado. O valor é
	 * usado somente em memória para o rate limit e nunca é gravado.
	 */
	public String resolve(HttpServletRequest request) {
		String headerName = properties.getIpHeader();
		if (StringUtils.hasText(headerName)) {
			String headerValue = request.getHeader(headerName);
			if (StringUtils.hasText(headerValue)) {
				String first = headerValue.split(",")[0].strip();
				if (StringUtils.hasText(first)) {
					return first;
				}
			}
		}
		String remote = request.getRemoteAddr();
		return StringUtils.hasText(remote) ? remote : "unknown";
	}
}
