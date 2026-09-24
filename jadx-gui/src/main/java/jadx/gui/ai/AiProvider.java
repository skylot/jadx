package jadx.gui.ai;

/**
 * Preset AI providers exposing an OpenAI-compatible "chat/completions" endpoint.
 * {@link #CUSTOM} allows pointing to any other compatible endpoint (local models, other clouds, ...).
 */
public enum AiProvider {
	GEMINI("Gemini (Google AI Studio)", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash"),
	OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
	CUSTOM("Custom / OpenAI-compatible", "", "");

	private final String label;
	private final String defaultBaseUrl;
	private final String defaultModel;

	AiProvider(String label, String defaultBaseUrl, String defaultModel) {
		this.label = label;
		this.defaultBaseUrl = defaultBaseUrl;
		this.defaultModel = defaultModel;
	}

	public String getDefaultBaseUrl() {
		return defaultBaseUrl;
	}

	public String getDefaultModel() {
		return defaultModel;
	}

	@Override
	public String toString() {
		return label;
	}
}
