package territorio_limpo_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

	private String supabaseUrl = "";
	private String serviceKey = "";
	private String bucket = "ocorrencia-fotos";
	private Duration signedUrlTtl = Duration.ofMinutes(10);
	private Duration connectTimeout = Duration.ofSeconds(2);
	private Duration readTimeout = Duration.ofSeconds(10);

	public String getSupabaseUrl() {
		return supabaseUrl;
	}

	public void setSupabaseUrl(String supabaseUrl) {
		this.supabaseUrl = supabaseUrl;
	}

	public String getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public Duration getSignedUrlTtl() {
		return signedUrlTtl;
	}

	public void setSignedUrlTtl(Duration signedUrlTtl) {
		this.signedUrlTtl = signedUrlTtl;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isConfigured() {
		return supabaseUrl != null && !supabaseUrl.isBlank()
				&& serviceKey != null && !serviceKey.isBlank()
				&& bucket != null && !bucket.isBlank();
	}
}
