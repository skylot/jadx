package jadx.mcp.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jadx.mcp.protocol.JsonRpc;
import jadx.mcp.protocol.McpVersions;
import jadx.mcp.tools.CancellationToken;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolRegistry;

/**
 * MCP lifecycle and JSON-RPC dispatch, independent of HTTP I/O.
 * Implements Streamable HTTP rules for session id and protocol version headers.
 */
public final class McpProtocolHandler {
	private static final Logger LOG = LoggerFactory.getLogger(McpProtocolHandler.class);

	public static final String SERVER_NAME = "jadx";

	private final McpServerConfig config;
	private final ToolRegistry registry;
	private final McpSessionManager sessions;
	private final ToolExecutor executor;
	private final Set<CancellationToken> activeTokens = ConcurrentHashMap.newKeySet();
	private final AtomicLong requestsCount = new AtomicLong();
	private final AtomicLong toolCallsCount = new AtomicLong();
	private volatile @Nullable String lastClient;
	private volatile boolean stopped;

	public McpProtocolHandler(McpServerConfig config, ToolRegistry registry, McpSessionManager sessions, ToolExecutor executor) {
		this.config = config;
		this.registry = registry;
		this.sessions = sessions;
		this.executor = executor;
	}

	/**
	 * Transport response: HTTP status, optional JSON body and new session id
	 */
	public static final class Response {
		private final int status;
		private final @Nullable JsonElement body;
		private final @Nullable String sessionId;

		Response(int status, @Nullable JsonElement body, @Nullable String sessionId) {
			this.status = status;
			this.body = body;
			this.sessionId = sessionId;
		}

		static Response json(JsonElement body) {
			return new Response(200, body, null);
		}

		static Response accepted() {
			return new Response(202, null, null);
		}

		static Response error(int status, @Nullable JsonElement id, int code, String message) {
			return new Response(status, JsonRpc.error(id, code, message), null);
		}

		public int getStatus() {
			return status;
		}

		public @Nullable JsonElement getBody() {
			return body;
		}

		public @Nullable String getSessionId() {
			return sessionId;
		}
	}

	public Response handlePost(@Nullable String sessionHeader, @Nullable String versionHeader, JsonElement message) {
		requestsCount.incrementAndGet();
		if (stopped) {
			return Response.error(503, null, JsonRpc.SERVER_BUSY, "Server is stopping");
		}
		if (message.isJsonObject() && isInitializeRequest(message.getAsJsonObject())) {
			return handleInitialize(message.getAsJsonObject());
		}
		JsonElement msgId = message.isJsonObject() ? validIdOrNull(message.getAsJsonObject().get("id")) : null;
		if (sessionHeader == null || sessionHeader.isEmpty()) {
			return Response.error(400, msgId, JsonRpc.INVALID_REQUEST,
					"Missing Mcp-Session-Id header. Send 'initialize' request first.");
		}
		McpSession session = sessions.get(sessionHeader);
		if (session == null) {
			return Response.error(404, msgId, JsonRpc.INVALID_REQUEST,
					"Session not found or expired. Start a new session with 'initialize' request.");
		}
		if (versionHeader != null && !McpVersions.isSupported(versionHeader)) {
			return Response.error(400, msgId, JsonRpc.INVALID_REQUEST,
					"Unsupported MCP-Protocol-Version: " + versionHeader + ", supported: " + McpVersions.SUPPORTED);
		}
		if (message.isJsonArray()) {
			return handleBatch(session, message.getAsJsonArray());
		}
		if (!message.isJsonObject()) {
			return Response.error(400, null, JsonRpc.INVALID_REQUEST, "JSON-RPC message must be an object");
		}
		JsonObject response = handleMessage(session, message.getAsJsonObject());
		if (response == null) {
			return Response.accepted();
		}
		return Response.json(response);
	}

	public Response handleDelete(@Nullable String sessionHeader) {
		if (sessionHeader == null || sessionHeader.isEmpty()) {
			return Response.error(400, null, JsonRpc.INVALID_REQUEST, "Missing Mcp-Session-Id header");
		}
		if (sessions.remove(sessionHeader, CancellationToken.Reason.SESSION_CLOSED)) {
			return new Response(200, null, null);
		}
		return Response.error(404, null, JsonRpc.INVALID_REQUEST, "Session not found");
	}

	private Response handleBatch(McpSession session, JsonArray batch) {
		if (!McpVersions.isBatchAllowed(session.getProtocolVersion())) {
			return Response.error(400, null, JsonRpc.INVALID_REQUEST,
					"JSON-RPC batching is not supported in protocol version " + session.getProtocolVersion());
		}
		if (batch.isEmpty()) {
			return Response.error(400, null, JsonRpc.INVALID_REQUEST, "Empty batch");
		}
		JsonArray responses = new JsonArray();
		for (JsonElement element : batch) {
			if (!element.isJsonObject()) {
				responses.add(JsonRpc.error(null, JsonRpc.INVALID_REQUEST, "JSON-RPC message must be an object"));
				continue;
			}
			JsonObject msg = element.getAsJsonObject();
			if (isInitializeRequest(msg)) {
				responses.add(JsonRpc.error(msg.get("id"), JsonRpc.INVALID_REQUEST, "'initialize' is not allowed in batch"));
				continue;
			}
			JsonObject response = handleMessage(session, msg);
			if (response != null) {
				responses.add(response);
			}
		}
		if (responses.isEmpty()) {
			return Response.accepted();
		}
		return Response.json(responses);
	}

	private static boolean isInitializeRequest(JsonObject msg) {
		JsonElement method = msg.get("method");
		return method != null && method.isJsonPrimitive()
				&& "initialize".equals(method.getAsString())
				&& msg.has("id");
	}

	private Response handleInitialize(JsonObject request) {
		JsonElement id = request.get("id");
		if (!JsonRpc.isValidId(id)) {
			return Response.error(400, null, JsonRpc.INVALID_REQUEST, "Invalid request id");
		}
		JsonElement paramsEl = request.get("params");
		if (paramsEl == null || !paramsEl.isJsonObject()) {
			return Response.json(JsonRpc.error(id, JsonRpc.INVALID_PARAMS, "Missing 'params' object"));
		}
		JsonObject params = paramsEl.getAsJsonObject();
		JsonElement versionEl = params.get("protocolVersion");
		if (versionEl == null || !versionEl.isJsonPrimitive() || !versionEl.getAsJsonPrimitive().isString()) {
			return Response.json(JsonRpc.error(id, JsonRpc.INVALID_PARAMS, "Missing 'protocolVersion' string"));
		}
		String version = McpVersions.negotiate(versionEl.getAsString());
		JsonElement clientInfoEl = params.get("clientInfo");
		JsonObject clientInfo = clientInfoEl != null && clientInfoEl.isJsonObject() ? clientInfoEl.getAsJsonObject() : null;

		McpSession session = new McpSession(sessions.newSessionId(), version, clientInfo);
		sessions.add(session);
		lastClient = session.getClientName();
		LOG.info("MCP client connected: {}, protocol: {} (requested: {})",
				session.getClientName(), version, versionEl.getAsString());

		JsonObject result = new JsonObject();
		result.addProperty("protocolVersion", version);
		JsonObject capabilities = new JsonObject();
		JsonObject tools = new JsonObject();
		tools.addProperty("listChanged", false);
		capabilities.add("tools", tools);
		result.add("capabilities", capabilities);
		JsonObject serverInfo = new JsonObject();
		serverInfo.addProperty("name", SERVER_NAME);
		serverInfo.addProperty("title", "jadx");
		serverInfo.addProperty("version", config.getServerVersion());
		serverInfo.addProperty("websiteUrl", "https://github.com/skylot/jadx");
		result.add("serverInfo", serverInfo);
		String instructions = config.getInstructions();
		if (instructions != null) {
			result.addProperty("instructions", instructions);
		}
		return new Response(200, JsonRpc.result(id, result), session.getId());
	}

	/**
	 * @return response for request, null for notification or response messages
	 */
	@Nullable
	JsonObject handleMessage(McpSession session, JsonObject msg) {
		JsonElement versionEl = msg.get("jsonrpc");
		JsonElement methodEl = msg.get("method");
		boolean hasId = msg.has("id");
		if (versionEl == null || !JsonRpc.VERSION.equals(safeString(versionEl))) {
			return hasId ? JsonRpc.error(validIdOrNull(msg.get("id")), JsonRpc.INVALID_REQUEST, "Expected jsonrpc: '2.0'") : null;
		}
		if (methodEl == null) {
			if (hasId && (msg.has("result") || msg.has("error"))) {
				// response to server request (server doesn't send requests), ignore
				return null;
			}
			return JsonRpc.error(validIdOrNull(msg.get("id")), JsonRpc.INVALID_REQUEST, "Missing 'method'");
		}
		String method = safeString(methodEl);
		if (method == null) {
			return JsonRpc.error(validIdOrNull(msg.get("id")), JsonRpc.INVALID_REQUEST, "'method' must be a string");
		}
		JsonElement paramsEl = msg.get("params");
		if (paramsEl != null && !paramsEl.isJsonNull() && !paramsEl.isJsonObject()) {
			return hasId ? JsonRpc.error(validIdOrNull(msg.get("id")), JsonRpc.INVALID_PARAMS, "'params' must be an object") : null;
		}
		JsonObject params = paramsEl != null && paramsEl.isJsonObject() ? paramsEl.getAsJsonObject() : new JsonObject();
		if (!hasId) {
			handleNotification(session, method, params);
			return null;
		}
		JsonElement id = msg.get("id");
		if (!JsonRpc.isValidId(id)) {
			return JsonRpc.error(null, JsonRpc.INVALID_REQUEST, "Request id must be a string or integer");
		}
		try {
			return handleRequest(session, id, method, params);
		} catch (Exception e) {
			LOG.error("MCP request '{}' failed", method, e);
			return JsonRpc.error(id, JsonRpc.INTERNAL_ERROR, "Internal error: " + e.getMessage());
		}
	}

	private JsonObject handleRequest(McpSession session, JsonElement id, String method, JsonObject params) {
		switch (method) {
			case "ping":
				return JsonRpc.result(id, new JsonObject());

			case "tools/list": {
				JsonObject result = new JsonObject();
				result.add("tools", registry.listJson(McpVersions.isStructuredOutputSupported(session.getProtocolVersion())));
				return JsonRpc.result(id, result);
			}

			case "tools/call":
				return callTool(session, id, params);

			case "initialize":
				return JsonRpc.error(id, JsonRpc.INVALID_REQUEST, "Session already initialized");

			default:
				return JsonRpc.error(id, JsonRpc.METHOD_NOT_FOUND, "Method not found: " + method);
		}
	}

	private JsonObject callTool(McpSession session, JsonElement id, JsonObject params) {
		String name = safeString(params.get("name"));
		if (name == null) {
			return JsonRpc.error(id, JsonRpc.INVALID_PARAMS, "Missing tool 'name'");
		}
		McpTool tool = registry.get(name);
		if (tool == null) {
			return JsonRpc.error(id, JsonRpc.INVALID_PARAMS, "Unknown tool: " + name);
		}
		JsonElement argsEl = params.get("arguments");
		if (argsEl != null && !argsEl.isJsonNull() && !argsEl.isJsonObject()) {
			return JsonRpc.error(id, JsonRpc.INVALID_PARAMS, "'arguments' must be an object");
		}
		JsonObject args = argsEl != null && argsEl.isJsonObject() ? argsEl.getAsJsonObject() : new JsonObject();
		String idKey = JsonRpc.idKey(id);
		CancellationToken token = new CancellationToken();
		if (!session.addInFlight(idKey, token)) {
			return JsonRpc.error(id, JsonRpc.INVALID_REQUEST, "Request with same id is already running");
		}
		activeTokens.add(token);
		toolCallsCount.incrementAndGet();
		long start = System.currentTimeMillis();
		try {
			long timeout = config.getToolTimeoutMs();
			boolean structured = McpVersions.isStructuredOutputSupported(session.getProtocolVersion());
			ToolCallContext ctx = new ToolCallContext(token, session.getId(), timeout);
			JsonObject result = executor.execute(() -> registry.call(tool, args, ctx, structured), token, timeout);
			if (token.getReason() == CancellationToken.Reason.CLIENT_CANCELLED) {
				// client already ignores the response, reply to close HTTP exchange
				return JsonRpc.error(id, JsonRpc.REQUEST_CANCELLED, "Request cancelled");
			}
			return JsonRpc.result(id, result);
		} finally {
			activeTokens.remove(token);
			session.removeInFlight(idKey);
			if (LOG.isDebugEnabled()) {
				LOG.debug("MCP tool '{}' done in {} ms", name, System.currentTimeMillis() - start);
			}
		}
	}

	private void handleNotification(McpSession session, String method, JsonObject params) {
		switch (method) {
			case "notifications/initialized":
				session.setInitialized();
				break;

			case "notifications/cancelled": {
				JsonElement requestId = params.get("requestId");
				if (JsonRpc.isValidId(requestId)) {
					boolean found = session.cancelInFlight(JsonRpc.idKey(requestId));
					LOG.debug("MCP cancel request {}: {}", requestId, found ? "cancelled" : "not running");
				}
				break;
			}

			default:
				LOG.debug("Ignore MCP notification: {}", method);
				break;
		}
	}

	/**
	 * Cancel all running calls and reject new ones
	 */
	public void stop() {
		stopped = true;
		for (CancellationToken token : activeTokens) {
			token.cancel(CancellationToken.Reason.SERVER_STOPPED);
		}
		sessions.closeAll(CancellationToken.Reason.SERVER_STOPPED);
	}

	public long getRequestsCount() {
		return requestsCount.get();
	}

	public long getToolCallsCount() {
		return toolCallsCount.get();
	}

	public int getActiveCallsCount() {
		return activeTokens.size();
	}

	public @Nullable String getLastClient() {
		return lastClient;
	}

	private static @Nullable String safeString(@Nullable JsonElement el) {
		if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
			return el.getAsString();
		}
		return null;
	}

	private static @Nullable JsonElement validIdOrNull(@Nullable JsonElement id) {
		return JsonRpc.isValidId(id) ? id : null;
	}
}
