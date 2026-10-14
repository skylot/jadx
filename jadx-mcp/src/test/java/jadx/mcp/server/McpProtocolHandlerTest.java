package jadx.mcp.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.mcp.protocol.JsonRpc;
import jadx.mcp.protocol.McpVersions;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;
import jadx.mcp.tools.ToolRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class McpProtocolHandlerTest {
	private final AtomicBoolean writeEnabled = new AtomicBoolean(false);
	private final CountDownLatch slowStarted = new CountDownLatch(1);
	private final AtomicBoolean slowCancelled = new AtomicBoolean(false);
	private ToolRegistry registry;
	private McpProtocolHandler handler;
	private ToolExecutor executor;

	@BeforeEach
	void init() {
		ToolRegistry registry = new ToolRegistry(writeEnabled::get);
		registry.register(McpTool.builder("echo")
				.description("Echo")
				.input(Schema.object().required("text", Schema.string("text")).build())
				.output(Schema.object().open().required("text", Schema.string("text")).build())
				.handler((ctx, args) -> {
					JsonObject obj = new JsonObject();
					obj.addProperty("text", args.getString("text"));
					return obj;
				})
				.build());
		registry.register(McpTool.builder("fail")
				.description("Fail")
				.handler((ctx, args) -> {
					throw new ToolException(ToolErrorCode.NOT_FOUND, "nothing");
				})
				.build());
		registry.register(McpTool.builder("slow")
				.description("Slow")
				.handler((ctx, args) -> {
					slowStarted.countDown();
					while (true) {
						if (ctx.getToken().isCancelled()) {
							slowCancelled.set(true);
						}
						ctx.checkCancelled();
						sleep(10);
					}
				})
				.build());
		registry.register(McpTool.builder("modify")
				.description("Write tool")
				.write(false, true)
				.handler((ctx, args) -> new JsonObject())
				.build());
		this.registry = registry;
		createHandler(60_000);
	}

	private void createHandler(long toolTimeoutMs) {
		if (handler != null) {
			handler.stop();
			executor.shutdown();
		}
		McpServerConfig config = McpServerConfig.builder()
				.toolTimeoutMs(toolTimeoutMs).serverVersion("test").instructions("hi").build();
		executor = new ToolExecutor(2);
		handler = new McpProtocolHandler(config, registry, new McpSessionManager(4, 60_000), executor);
	}

	@AfterEach
	void stop() {
		handler.stop();
		executor.shutdown();
	}

	private static JsonElement json(String str) {
		return JsonParser.parseString(str);
	}

	private String initSession(String version) {
		McpProtocolHandler.Response resp = handler.handlePost(null, null, json(
				"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"" + version + "\","
						+ "\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}"));
		assertThat(resp.getStatus()).isEqualTo(200);
		assertThat(resp.getSessionId()).isNotNull();
		JsonObject result = resp.getBody().getAsJsonObject().getAsJsonObject("result");
		assertThat(result.get("protocolVersion").getAsString()).isEqualTo(McpVersions.negotiate(version));
		McpProtocolHandler.Response initialized = handler.handlePost(resp.getSessionId(), version,
				json("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
		assertThat(initialized.getStatus()).isEqualTo(202);
		assertThat(initialized.getBody()).isNull();
		return resp.getSessionId();
	}

	private JsonObject request(String session, String body) {
		McpProtocolHandler.Response resp = handler.handlePost(session, McpVersions.LATEST, json(body));
		assertThat(resp.getStatus()).isEqualTo(200);
		return resp.getBody().getAsJsonObject();
	}

	@Test
	void initializeAndNegotiate() {
		McpProtocolHandler.Response resp = handler.handlePost(null, null, json(
				"{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2099-01-01\","
						+ "\"capabilities\":{}}}"));
		JsonObject result = resp.getBody().getAsJsonObject().getAsJsonObject("result");
		assertThat(result.get("protocolVersion").getAsString()).isEqualTo(McpVersions.LATEST);
		assertThat(result.getAsJsonObject("capabilities").has("tools")).isTrue();
		assertThat(result.getAsJsonObject("serverInfo").get("name").getAsString()).isEqualTo("jadx");
		assertThat(result.get("instructions").getAsString()).isEqualTo("hi");
		assertThat(resp.getBody().getAsJsonObject().get("id").getAsString()).isEqualTo("a");

		for (String version : McpVersions.SUPPORTED) {
			initSession(version);
		}
		McpProtocolHandler.Response noParams = handler.handlePost(null, null,
				json("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}"));
		assertThat(noParams.getBody().getAsJsonObject().getAsJsonObject("error").get("code").getAsInt())
				.isEqualTo(JsonRpc.INVALID_PARAMS);
	}

	@Test
	void sessionChecks() {
		McpProtocolHandler.Response missing = handler.handlePost(null, null, json("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"));
		assertThat(missing.getStatus()).isEqualTo(400);
		McpProtocolHandler.Response unknown = handler.handlePost("bad", null, json("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"));
		assertThat(unknown.getStatus()).isEqualTo(404);
		String session = initSession(McpVersions.LATEST);
		McpProtocolHandler.Response badVersion = handler.handlePost(session, "1999-01-01",
				json("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"));
		assertThat(badVersion.getStatus()).isEqualTo(400);

		JsonObject pong = request(session, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"ping\"}");
		assertThat(pong.get("id").getAsInt()).isEqualTo(7);
		assertThat(pong.getAsJsonObject("result").size()).isZero();

		assertThat(handler.handleDelete(session).getStatus()).isEqualTo(200);
		assertThat(handler.handlePost(session, null, json("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}")).getStatus())
				.isEqualTo(404);
		assertThat(handler.handleDelete(session).getStatus()).isEqualTo(404);
	}

	@Test
	void jsonRpcErrors() {
		String session = initSession(McpVersions.LATEST);
		JsonObject unknownMethod = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/list\"}");
		assertThat(unknownMethod.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.METHOD_NOT_FOUND);
		JsonObject badVersion = request(session, "{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"ping\"}");
		assertThat(badVersion.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.INVALID_REQUEST);
		JsonObject badId = request(session, "{\"jsonrpc\":\"2.0\",\"id\":{},\"method\":\"ping\"}");
		assertThat(badId.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.INVALID_REQUEST);
		JsonObject badParams = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":[1]}");
		assertThat(badParams.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.INVALID_PARAMS);
		JsonObject reinit = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
		assertThat(reinit.has("error") || reinit.has("result")).isTrue();
		// client responses are accepted and ignored
		McpProtocolHandler.Response response = handler.handlePost(session, null, json("{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{}}"));
		assertThat(response.getStatus()).isEqualTo(202);
	}

	@Test
	void batching() {
		String latest = initSession(McpVersions.LATEST);
		McpProtocolHandler.Response rejected = handler.handlePost(latest, null,
				json("[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}]"));
		assertThat(rejected.getStatus()).isEqualTo(400);

		String old = initSession(McpVersions.V2025_03_26);
		McpProtocolHandler.Response batch = handler.handlePost(old, null, json(
				"[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"},"
						+ "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"},"
						+ "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}]"));
		assertThat(batch.getStatus()).isEqualTo(200);
		assertThat(batch.getBody().getAsJsonArray()).hasSize(2);
	}

	@Test
	void toolsListAndCall() {
		String session = initSession(McpVersions.LATEST);
		JsonObject list = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
		String tools = list.getAsJsonObject("result").getAsJsonArray("tools").toString();
		assertThat(tools).contains("\"echo\"", "outputSchema", "readOnlyHint").doesNotContain("\"modify\"");

		JsonObject echo = request(session, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"hi\"}}}");
		JsonObject result = echo.getAsJsonObject("result");
		assertThat(result.get("isError").getAsBoolean()).isFalse();
		assertThat(result.getAsJsonObject("structuredContent").get("text").getAsString()).isEqualTo("hi");
		assertThat(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString()).contains("hi");

		JsonObject invalidArgs = request(session, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":1}}}");
		assertThat(invalidArgs.getAsJsonObject("result").get("isError").getAsBoolean()).isTrue();

		JsonObject unknownTool = request(session, "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"nope\"}}");
		assertThat(unknownTool.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.INVALID_PARAMS);

		JsonObject fail = request(session, "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"fail\"}}");
		assertThat(fail.getAsJsonObject("result").get("isError").getAsBoolean()).isTrue();
		assertThat(fail.toString()).contains("NOT_FOUND");

		JsonObject write = request(session, "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"modify\"}}");
		assertThat(write.toString()).contains("WRITE_DISABLED");
		writeEnabled.set(true);
		JsonObject listWithWrite = request(session, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/list\"}");
		assertThat(listWithWrite.toString()).contains("\"modify\"", "destructiveHint");
	}

	@Test
	void noStructuredOutputForOldProtocol() {
		String session = initSession(McpVersions.V2025_03_26);
		JsonObject list = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
		assertThat(list.toString()).doesNotContain("outputSchema");
		JsonObject echo = request(session, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"hi\"}}}");
		assertThat(echo.getAsJsonObject("result").has("structuredContent")).isFalse();
	}

	@Test
	void timeout() {
		createHandler(1500);
		String session = initSession(McpVersions.LATEST);
		long start = System.currentTimeMillis();
		JsonObject slow = request(session, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"slow\"}}");
		assertThat(System.currentTimeMillis() - start).isLessThan(15_000);
		assertThat(slow.getAsJsonObject("result").get("isError").getAsBoolean()).isTrue();
		assertThat(slow.toString()).contains("TIMEOUT");
		// under heavy load the task can time out while still queued, then it is never started
		if (slowStarted.getCount() == 0) {
			waitFor(slowCancelled);
		}
	}

	@Test
	void cancellation() throws Exception {
		String session = initSession(McpVersions.LATEST);
		CompletableFuture<JsonObject> call = new CompletableFuture<>();
		Thread caller = new Thread(() -> call.complete(request(session,
				"{\"jsonrpc\":\"2.0\",\"id\":\"req-1\",\"method\":\"tools/call\",\"params\":{\"name\":\"slow\"}}")));
		caller.setDaemon(true);
		caller.start();
		assertThat(slowStarted.await(30, TimeUnit.SECONDS)).isTrue();
		McpProtocolHandler.Response cancel = handler.handlePost(session, null, json(
				"{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":\"req-1\",\"reason\":\"user\"}}"));
		assertThat(cancel.getStatus()).isEqualTo(202);
		JsonObject response = call.get(30, TimeUnit.SECONDS);
		assertThat(response.getAsJsonObject("error").get("code").getAsInt()).isEqualTo(JsonRpc.REQUEST_CANCELLED);
		waitFor(slowCancelled);
	}

	private static void waitFor(AtomicBoolean flag) {
		long end = System.currentTimeMillis() + 30_000;
		while (!flag.get() && System.currentTimeMillis() < end) {
			sleep(10);
		}
		assertThat(flag.get()).as("worker observed cancellation").isTrue();
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
