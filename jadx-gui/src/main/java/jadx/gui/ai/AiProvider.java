package jadx.gui.ai;

/**
 * Preset AI providers exposing an OpenAI-compatible "chat/completions" endpoint.
 * {@link #CUSTOM} allows pointing to any other compatible endpoint (local models, other clouds, ...).
 */
public enum AiProvider {
	GEMINI("Gemini (Google AI Studio)", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash",
			"https://aistudio.google.com/apikey"),
	OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "https://platform.openai.com/api-keys"),
	CUSTOM("Custom / OpenAI-compatible", "", "", null);

	private final String label;
	private final String defaultBaseUrl;
	private final String defaultModel;
	private final String apiKeyUrl;

	AiProvider(String label, String defaultBaseUrl, String defaultModel, String apiKeyUrl) {
		this.label = label;
		this.defaultBaseUrl = defaultBaseUrl;
		this.defaultModel = defaultModel;
		this.apiKeyUrl = apiKeyUrl;
	}

	public String getDefaultBaseUrl() {
		return defaultBaseUrl;
	}

	public String getDefaultModel() {
		return defaultModel;
	}

	/**
	 * Page to get a free/paid API key from, or null if this provider has none (e.g. {@link #CUSTOM}).
	 */
	public String getApiKeyUrl() {
		return apiKeyUrl;
	}

	@Override
	public String toString() {
		return label;
	}
}
