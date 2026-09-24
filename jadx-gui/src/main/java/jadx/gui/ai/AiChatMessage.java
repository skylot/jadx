package jadx.gui.ai;

/**
 * Single chat message, matches the OpenAI-compatible "messages" array format.
 */
public class AiChatMessage {
	public static final String ROLE_SYSTEM = "system";
	public static final String ROLE_USER = "user";
	public static final String ROLE_ASSISTANT = "assistant";

	private final String role;
	private final String content;

	public AiChatMessage(String role, String content) {
		this.role = role;
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}
}
