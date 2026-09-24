package jadx.gui.ai;

/**
 * Persisted AI Assistant configuration (stored inside jadx-gui settings JSON).
 * Network access (proxy, custom trusted CA certificate) must be configured manually,
 * jadx does not attempt to auto-detect a system proxy or filtering software.
 */
public class AiSettings {
	private boolean enabled = false;
	private AiProvider provider = AiProvider.GEMINI;
	private String baseUrl = AiProvider.GEMINI.getDefaultBaseUrl();
	private String model = AiProvider.GEMINI.getDefaultModel();
	private String apiKey = "";

	private String proxyHost = "";
	private String proxyPort = "";
	private String proxyUsername = "";
	private String proxyPassword = "";
	private String customCaCertPath = "";
	private boolean trustSystemCertStore = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public AiProvider getProvider() {
		return provider;
	}

	public void setProvider(AiProvider provider) {
		this.provider = provider;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public String getCustomCaCertPath() {
		return customCaCertPath;
	}

	public void setCustomCaCertPath(String customCaCertPath) {
		this.customCaCertPath = customCaCertPath;
	}

	public boolean isTrustSystemCertStore() {
		return trustSystemCertStore;
	}

	public void setTrustSystemCertStore(boolean trustSystemCertStore) {
		this.trustSystemCertStore = trustSystemCertStore;
	}
}
