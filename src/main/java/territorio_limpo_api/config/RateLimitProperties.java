package territorio_limpo_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

	private boolean enabled = true;
	private int perIpLimit = 5;
	private Duration perIpWindow = Duration.ofHours(1);
	private int globalLimit = 100;
	private Duration globalWindow = Duration.ofHours(1);
	private String ipHeader = "X-Forwarded-For";
	private Duration entryTtl = Duration.ofHours(24);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPerIpLimit() {
		return perIpLimit;
	}

	public void setPerIpLimit(int perIpLimit) {
		this.perIpLimit = perIpLimit;
	}

	public Duration getPerIpWindow() {
		return perIpWindow;
	}

	public void setPerIpWindow(Duration perIpWindow) {
		this.perIpWindow = perIpWindow;
	}

	public int getGlobalLimit() {
		return globalLimit;
	}

	public void setGlobalLimit(int globalLimit) {
		this.globalLimit = globalLimit;
	}

	public Duration getGlobalWindow() {
		return globalWindow;
	}

	public void setGlobalWindow(Duration globalWindow) {
		this.globalWindow = globalWindow;
	}

	public String getIpHeader() {
		return ipHeader;
	}

	public void setIpHeader(String ipHeader) {
		this.ipHeader = ipHeader;
	}

	public Duration getEntryTtl() {
		return entryTtl;
	}

	public void setEntryTtl(Duration entryTtl) {
		this.entryTtl = entryTtl;
	}
}
