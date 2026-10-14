package jadx.mcp.jadx;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.mcp.samples.SampleService;
import jadx.mcp.tools.CancellationToken;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decompiler with test sample classes, headless backend and tools registry
 */
public final class SampleProject implements AutoCloseable {
	private final JadxDecompiler decompiler;
	private final HeadlessBackend backend;
	private final ToolRegistry registry;

	public SampleProject(boolean writeEnabled) {
		JadxArgs args = new JadxArgs();
		args.setInputFiles(sampleClassFiles());
		args.setThreadsCount(1);
		decompiler = new JadxDecompiler(args);
		decompiler.load();
		backend = new HeadlessBackend();
		backend.open(decompiler);
		registry = JadxMcpTools.createRegistry(backend, () -> writeEnabled);
		registry.setValidateOutput(true);
	}

	private static List<File> sampleClassFiles() {
		try {
			File dir = new File(Objects.requireNonNull(SampleService.class.getResource("SampleService.class")).toURI())
					.getParentFile();
			List<File> files = new ArrayList<>();
			for (File file : Objects.requireNonNull(dir.listFiles())) {
				if (file.getName().endsWith(".class")) {
					files.add(file);
				}
			}
			assertThat(files).hasSizeGreaterThanOrEqualTo(5);
			return files;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public JadxDecompiler getDecompiler() {
		return decompiler;
	}

	public HeadlessBackend getBackend() {
		return backend;
	}

	public ToolRegistry getRegistry() {
		return registry;
	}

	/**
	 * Call tool and return full CallToolResult
	 */
	public JsonObject callRaw(String toolName, String argsJson) {
		McpTool tool = registry.get(toolName);
		assertThat(tool).as("tool %s", toolName).isNotNull();
		JsonObject args = JsonParser.parseString(argsJson).getAsJsonObject();
		ToolCallContext ctx = new ToolCallContext(new CancellationToken(), "test", 60_000);
		return registry.call(tool, args, ctx, true);
	}

	/**
	 * Call tool, check success and return structured content
	 */
	public JsonObject call(String toolName, String argsJson) {
		JsonObject result = callRaw(toolName, argsJson);
		assertThat(result.get("isError").getAsBoolean())
				.as("tool %s failed: %s", toolName, result)
				.isFalse();
		return result.getAsJsonObject("structuredContent");
	}

	/**
	 * Call tool, check error and return error object
	 */
	public JsonObject callError(String toolName, String argsJson) {
		JsonObject result = callRaw(toolName, argsJson);
		assertThat(result.get("isError").getAsBoolean())
				.as("tool %s expected to fail: %s", toolName, result)
				.isTrue();
		String text = result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
		return JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("error");
	}

	@Override
	public void close() {
		backend.close();
		decompiler.close();
	}
}
