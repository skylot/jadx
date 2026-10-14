package jadx.mcp.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerHttpTest {
	private static final String TOKEN = "test-token-123";
	private static final String INIT = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
			+ "\"params\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"http-test\",\"version\":\"1\"}}}";

	private McpServer server;
	private HttpClient client;
	private String url;

	@BeforeEach
	void start() throws Exception {
		ToolRegistry registry = new ToolRegistry(() -> false);
		registry.register(McpTool.builder("hello")
				.description("Hello")
				.input(Schema.object().build())
				.handler((ctx, args) -> {
					JsonObject obj = new JsonObject();
					obj.addProperty("msg", "hello");
					return obj;
				})
				.build());
		server = new McpServer(McpServerConfig.builder().port(0).token(TOKEN).build(), registry);
		server.start();
		url = server.getEndpointUrl();
		client = HttpClient.newHttpClient();
	}

	@AfterEach
	void stop() {
		server.stop();
	}

	private HttpRequest.Builder post(String body) {
		return HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/event-stream")
				.POST(HttpRequest.BodyPublishers.ofString(body));
	}

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void fullFlow() throws Exception {
		HttpResponse<String> init = send(post(INIT).header("Authorization", "Bearer " + TOKEN).build());
		assertThat(init.statusCode()).isEqualTo(200);
		assertThat(init.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
		String session = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
		assertThat(session).matches("[0-9a-f]{32}");
		assertThat(server.getSessionsCount()).isEqualTo(1);

		HttpResponse<String> initialized = send(post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
				.header("Authorization", "Bearer " + TOKEN)
				.header("Mcp-Session-Id", session)
				.header("MCP-Protocol-Version", "2025-11-25")
				.build());
		assertThat(initialized.statusCode()).isEqualTo(202);
		assertThat(initialized.body()).isEmpty();

		HttpResponse<String> call = send(post("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"hello\"}}")
				.header("Authorization", "Bearer " + TOKEN)
				.header("Mcp-Session-Id", session)
				.header("MCP-Protocol-Version", "2025-11-25")
				.build());
		assertThat(call.statusCode()).isEqualTo(200);
		JsonObject result = JsonParser.parseString(call.body()).getAsJsonObject().getAsJsonObject("result");
		assertThat(result.getAsJsonObject("structuredContent").get("msg").getAsString()).isEqualTo("hello");

		HttpResponse<String> get = send(HttpRequest.newBuilder(URI.create(url))
				.header("Authorization", "Bearer " + TOKEN)
				.header("Accept", "text/event-stream")
				.header("Mcp-Session-Id", session)
				.GET().build());
		assertThat(get.statusCode()).isEqualTo(405);

		HttpResponse<String> delete = send(HttpRequest.newBuilder(URI.create(url))
				.header("Authorization", "Bearer " + TOKEN)
				.header("Mcp-Session-Id", session)
				.DELETE().build());
		assertThat(delete.statusCode()).isEqualTo(200);
		assertThat(server.getSessionsCount()).isZero();

		HttpResponse<String> afterDelete = send(post("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\"}")
				.header("Authorization", "Bearer " + TOKEN)
				.header("Mcp-Session-Id", session)
				.build());
		assertThat(afterDelete.statusCode()).isEqualTo(404);
	}

	@Test
	void security() throws Exception {
		HttpResponse<String> noAuth = send(post(INIT).build());
		assertThat(noAuth.statusCode()).isEqualTo(401);
		assertThat(noAuth.headers().firstValue("WWW-Authenticate")).isPresent();

		HttpResponse<String> badAuth = send(post(INIT).header("Authorization", "Bearer wrong").build());
		assertThat(badAuth.statusCode()).isEqualTo(401);

		HttpResponse<String> badOrigin = send(post(INIT)
				.header("Authorization", "Bearer " + TOKEN)
				.header("Origin", "https://evil.example.com")
				.build());
		assertThat(badOrigin.statusCode()).isEqualTo(403);

		HttpResponse<String> localOrigin = send(post(INIT)
				.header("Authorization", "Bearer " + TOKEN)
				.header("Origin", "http://localhost:3000")
				.build());
		assertThat(localOrigin.statusCode()).isEqualTo(200);

		// DNS rebinding: non-local Host header
		String raw = rawRequest("POST /mcp HTTP/1.1\r\nHost: evil.example.com\r\nAuthorization: Bearer " + TOKEN
				+ "\r\nContent-Type: application/json\r\nContent-Length: " + INIT.length() + "\r\nConnection: close\r\n\r\n" + INIT);
		assertThat(raw).startsWith("HTTP/1.1 403");
	}

	@Test
	void transportErrors() throws Exception {
		HttpResponse<String> badType = send(HttpRequest.newBuilder(URI.create(url))
				.header("Authorization", "Bearer " + TOKEN)
				.header("Content-Type", "text/plain")
				.POST(HttpRequest.BodyPublishers.ofString(INIT)).build());
		assertThat(badType.statusCode()).isEqualTo(415);

		HttpResponse<String> badAccept = send(post(INIT)
				.header("Authorization", "Bearer " + TOKEN)
				.setHeader("Accept", "text/html")
				.build());
		assertThat(badAccept.statusCode()).isEqualTo(406);

		HttpResponse<String> parseError = send(post("{oops")
				.header("Authorization", "Bearer " + TOKEN).build());
		assertThat(parseError.statusCode()).isEqualTo(400);
		assertThat(parseError.body()).contains("-32700");

		HttpResponse<String> notFound = send(HttpRequest.newBuilder(URI.create(url.replace("/mcp", "/other")))
				.header("Authorization", "Bearer " + TOKEN)
				.GET().build());
		assertThat(notFound.statusCode()).isEqualTo(404);
	}

	@Test
	void stopAndRestart() throws Exception {
		server.stop();
		assertThat(server.isRunning()).isFalse();
		server.start();
		assertThat(server.isRunning()).isTrue();
		url = server.getEndpointUrl();
		HttpResponse<String> init = send(post(INIT).header("Authorization", "Bearer " + TOKEN).build());
		assertThat(init.statusCode()).isEqualTo(200);
	}

	@Test
	void requireTokenForRemoteBind() {
		McpServer remote = new McpServer(McpServerConfig.builder().host("0.0.0.0").port(0).build(), new ToolRegistry(() -> false));
		try {
			remote.start();
			throw new AssertionError("Expected failure");
		} catch (Exception e) {
			assertThat(e.getMessage()).contains("token");
		} finally {
			remote.stop();
		}
	}

	@Test
	void customTokenValidation() throws Exception {
		assertThat(McpServerConfig.isValidToken("my-Custom_token.123")).isTrue();
		assertThat(McpServerConfig.isValidToken("short")).isFalse();
		assertThat(McpServerConfig.isValidToken("has space here")).isFalse();
		assertThat(McpServerConfig.isValidToken("中文令牌中文令牌中文")).isFalse();
		try {
			McpServerConfig.builder().token("bad token!");
			throw new AssertionError("Expected failure");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("8-256");
		}
		// empty token disables authentication
		assertThat(McpServerConfig.builder().token("").build().getToken()).isNull();

		// custom token works for authentication
		server.stop();
		server = new McpServer(McpServerConfig.builder().port(0).token("my-Custom_token.123").build(),
				new ToolRegistry(() -> false));
		server.start();
		url = server.getEndpointUrl();
		assertThat(send(post(INIT).header("Authorization", "Bearer my-Custom_token.123").build()).statusCode()).isEqualTo(200);
		assertThat(send(post(INIT).header("Authorization", "Bearer " + TOKEN).build()).statusCode()).isEqualTo(401);
	}

	@Test
	void hostHelpers() {
		assertThat(McpServer.stripPort("localhost:8650")).isEqualTo("localhost");
		assertThat(McpServer.stripPort("[::1]:8650")).isEqualTo("[::1]");
		assertThat(McpServer.isLocalHostName("127.0.0.1")).isTrue();
		assertThat(McpServer.isLocalHostName("[::1]")).isTrue();
		assertThat(McpServer.isLocalHostName("192.168.1.2")).isFalse();
		assertThat(McpServer.buildEndpointUrl("0.0.0.0", 1)).isEqualTo("http://127.0.0.1:1/mcp");
	}

	private String rawRequest(String request) throws Exception {
		URI uri = URI.create(url);
		try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
			OutputStream out = socket.getOutputStream();
			out.write(request.getBytes(StandardCharsets.UTF_8));
			out.flush();
			InputStream in = socket.getInputStream();
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
