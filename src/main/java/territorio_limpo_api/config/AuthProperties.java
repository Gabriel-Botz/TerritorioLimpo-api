package territorio_limpo_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

	private String jwksUri = "";
	private String jwtSecret = "";
	private String issuer = "";
	private String adminRoleClaim = "app_role";
	private String adminRoleValue = "ADMIN";

	public String getJwksUri() {
		return jwksUri;
	}

	public void setJwksUri(String jwksUri) {
		this.jwksUri = jwksUri;
	}

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getAdminRoleClaim() {
		return adminRoleClaim;
	}

	public void setAdminRoleClaim(String adminRoleClaim) {
		this.adminRoleClaim = adminRoleClaim;
	}

	public String getAdminRoleValue() {
		return adminRoleValue;
	}

	public void setAdminRoleValue(String adminRoleValue) {
		this.adminRoleValue = adminRoleValue;
	}

	public boolean hasJwks() {
		return jwksUri != null && !jwksUri.isBlank();
	}

	public boolean hasSecret() {
		return jwtSecret != null && !jwtSecret.isBlank();
	}
}
