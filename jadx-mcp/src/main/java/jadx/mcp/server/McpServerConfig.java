package jadx.mcp.server;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable server configuration
 */
public final class McpServerConfig {
	public static final String DEFAULT_HOST = "127.0.0.1";
	public static final int DEFAULT_PORT = 8650;
	public static final String ENDPOINT_PATH = "/mcp";

	/**
	 * Bearer token chars (RFC 6750 'b64token'), safe to use in HTTP header and client configs
	 */
	private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9\\-._~+/=]{8,256}");

	private final String host;
	private final int port;
	private final @Nullable String token;
	private final long toolTimeoutMs;
	private final int maxConcurrentCalls;
	private final int maxSessions;
	private final long sessionIdleTimeoutMs;
	private final int maxRequestBytes;
	private final String serverVersion;
	private final @Nullable String instructions;

	private McpServerConfig(Builder b) {
		this.host = Objects.requireNonNull(b.host);
		this.port = b.port;
		this.token = b.token == null || b.token.isEmpty() ? null : b.token;
		this.toolTimeoutMs = b.toolTimeoutMs;
		this.maxConcurrentCalls = b.maxConcurrentCalls;
		this.maxSessions = b.maxSessions;
		this.sessionIdleTimeoutMs = b.sessionIdleTimeoutMs;
		this.maxRequestBytes = b.maxRequestBytes;
		this.serverVersion = b.serverVersion;
		this.instructions = b.instructions;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Valid custom access token: 8-256 chars from 'A-Z a-z 0-9 - . _ ~ + / ='
	 */
	public static boolean isValidToken(@Nullable String token) {
		return token != null && TOKEN_PATTERN.matcher(token).matches();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	/**
	 * Bearer token required in 'Authorization' header, null if authentication disabled
	 */
	public @Nullable String getToken() {
		return token;
	}

	public long getToolTimeoutMs() {
		return toolTimeoutMs;
	}

	public int getMaxConcurrentCalls() {
		return maxConcurrentCalls;
	}

	public int getMaxSessions() {
		return maxSessions;
	}

	public long getSessionIdleTimeoutMs() {
		return sessionIdleTimeoutMs;
	}

	public int getMaxRequestBytes() {
		return maxRequestBytes;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public @Nullable String getInstructions() {
		return instructions;
	}

	public static final class Builder {
		private String host = DEFAULT_HOST;
		private int port = DEFAULT_PORT;
		private @Nullable String token;
		private long toolTimeoutMs = 60_000;
		private int maxConcurrentCalls = 4;
		private int maxSessions = 32;
		private long sessionIdleTimeoutMs = 2 * 60 * 60 * 1000L;
		private int maxRequestBytes = 4 * 1024 * 1024;
		private String serverVersion = "dev";
		private @Nullable String instructions;

		public Builder host(String host) {
			this.host = host;
			return this;
		}

		public Builder port(int port) {
			if (port < 0 || port > 65535) {
				throw new IllegalArgumentException("Invalid port: " + port);
			}
			this.port = port;
			return this;
		}

		/**
		 * @param token access token, null or empty to disable authentication
		 * @throws IllegalArgumentException if token contains not allowed chars or too short
		 */
		public Builder token(@Nullable String token) {
			if (token != null && !token.isEmpty() && !isValidToken(token)) {
				throw new IllegalArgumentException(
						"Access token must be 8-256 characters of: A-Z a-z 0-9 - . _ ~ + / =");
			}
			this.token = token;
			return this;
		}

		public Builder toolTimeoutMs(long toolTimeoutMs) {
			this.toolTimeoutMs = toolTimeoutMs;
			return this;
		}

		public Builder maxConcurrentCalls(int maxConcurrentCalls) {
			this.maxConcurrentCalls = Math.max(1, maxConcurrentCalls);
			return this;
		}

		public Builder maxSessions(int maxSessions) {
			this.maxSessions = Math.max(1, maxSessions);
			return this;
		}

		public Builder sessionIdleTimeoutMs(long sessionIdleTimeoutMs) {
			this.sessionIdleTimeoutMs = sessionIdleTimeoutMs;
			return this;
		}

		public Builder maxRequestBytes(int maxRequestBytes) {
			this.maxRequestBytes = maxRequestBytes;
			return this;
		}

		public Builder serverVersion(String serverVersion) {
			this.serverVersion = serverVersion;
			return this;
		}

		public Builder instructions(@Nullable String instructions) {
			this.instructions = instructions;
			return this;
		}

		public McpServerConfig build() {
			return new McpServerConfig(this);
		}
	}
}
