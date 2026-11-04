package territorio_limpo_api.storage;

public interface ClienteStorage {

	boolean isConfigured();

	void upload(String path, byte[] content);

	void delete(String path);

	String signedUrl(String path);
}
