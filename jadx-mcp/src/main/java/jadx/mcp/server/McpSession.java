package jadx.mcp.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import jadx.mcp.tools.CancellationToken;

/**
 * Client session created by 'initialize' request
 */
public final class McpSession {
	private final String id;
	private final String protocolVersion;
	private final @Nullable JsonObject clientInfo;
	private final long createdAt;
	private final Map<String, CancellationToken> inFlight = new ConcurrentHashMap<>();
	private volatile long lastAccess;
	private volatile boolean initialized;
	private volatile boolean closed;

	public McpSession(String id, String protocolVersion, @Nullable JsonObject clientInfo) {
		this.id = id;
		this.protocolVersion = protocolVersion;
		this.clientInfo = clientInfo;
		this.createdAt = System.currentTimeMillis();
		this.lastAccess = createdAt;
	}

	public String getId() {
		return id;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public @Nullable JsonObject getClientInfo() {
		return clientInfo;
	}

	public String getClientName() {
		if (clientInfo != null && clientInfo.has("name")) {
			String name = clientInfo.get("name").getAsString();
			if (clientInfo.has("version")) {
				return name + ' ' + clientInfo.get("version").getAsString();
			}
			return name;
		}
		return "unknown";
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public long getLastAccess() {
		return lastAccess;
	}

	public void touch() {
		lastAccess = System.currentTimeMillis();
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized() {
		this.initialized = true;
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Register in-flight request
	 *
	 * @return false if request with same id already running
	 */
	boolean addInFlight(String idKey, CancellationToken token) {
		return inFlight.putIfAbsent(idKey, token) == null;
	}

	void removeInFlight(String idKey) {
		inFlight.remove(idKey);
	}

	boolean cancelInFlight(String idKey) {
		CancellationToken token = inFlight.get(idKey);
		if (token != null) {
			token.cancel(CancellationToken.Reason.CLIENT_CANCELLED);
			return true;
		}
		return false;
	}

	public int getInFlightCount() {
		return inFlight.size();
	}

	void close(CancellationToken.Reason reason) {
		closed = true;
		for (CancellationToken token : inFlight.values()) {
			token.cancel(reason);
		}
	}

	@Override
	public String toString() {
		return "McpSession{" + id + ", client=" + getClientName() + ", version=" + protocolVersion + '}';
	}
}
