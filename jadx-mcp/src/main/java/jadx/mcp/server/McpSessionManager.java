package jadx.mcp.server;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.mcp.tools.CancellationToken;

/**
 * Sessions registry with idle expiration and count limit
 */
public final class McpSessionManager {
	private static final Logger LOG = LoggerFactory.getLogger(McpSessionManager.class);

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();
	private final int maxSessions;
	private final long idleTimeoutMs;

	public McpSessionManager(int maxSessions, long idleTimeoutMs) {
		this.maxSessions = maxSessions;
		this.idleTimeoutMs = idleTimeoutMs;
	}

	public String newSessionId() {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		char[] chars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			chars[i * 2] = HEX[v >>> 4];
			chars[i * 2 + 1] = HEX[v & 0x0F];
		}
		return new String(chars);
	}

	public synchronized void add(McpSession session) {
		removeExpired();
		if (sessions.size() >= maxSessions) {
			// evict least recently used session without running requests
			sessions.values().stream()
					.filter(s -> s.getInFlightCount() == 0)
					.min(Comparator.comparingLong(McpSession::getLastAccess))
					.ifPresent(s -> remove(s.getId(), CancellationToken.Reason.SESSION_CLOSED));
		}
		sessions.put(session.getId(), session);
		LOG.debug("MCP session created: {}", session);
	}

	public @Nullable McpSession get(String id) {
		McpSession session = sessions.get(id);
		if (session == null) {
			return null;
		}
		if (isExpired(session, System.currentTimeMillis())) {
			remove(id, CancellationToken.Reason.SESSION_CLOSED);
			return null;
		}
		session.touch();
		return session;
	}

	public boolean remove(String id, CancellationToken.Reason reason) {
		McpSession session = sessions.remove(id);
		if (session != null) {
			session.close(reason);
			LOG.debug("MCP session closed: {}", session);
			return true;
		}
		return false;
	}

	public void closeAll(CancellationToken.Reason reason) {
		for (String id : new ArrayList<>(sessions.keySet())) {
			remove(id, reason);
		}
	}

	public int size() {
		return sessions.size();
	}

	public List<McpSession> getSessions() {
		return new ArrayList<>(sessions.values());
	}

	private void removeExpired() {
		long now = System.currentTimeMillis();
		for (McpSession session : new ArrayList<>(sessions.values())) {
			if (isExpired(session, now)) {
				remove(session.getId(), CancellationToken.Reason.SESSION_CLOSED);
			}
		}
	}

	private boolean isExpired(McpSession session, long now) {
		return idleTimeoutMs > 0
				&& session.getInFlightCount() == 0
				&& now - session.getLastAccess() > idleTimeoutMs;
	}
}
