package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.SchemaValidator;

import static org.assertj.core.api.Assertions.assertThat;

class JadxToolsTest {
	private static final String SERVICE = "jadx.mcp.samples.SampleService";
	private static final String HELPER = "jadx.mcp.samples.SampleService$Helper";
	private static final String USER = "jadx.mcp.samples.SampleUser";

	private static SampleProject project;

	@BeforeAll
	static void init() {
		project = new SampleProject(false);
	}

	@AfterAll
	static void close() {
		project.close();
	}

	@Test
	void toolSchemasAreValid() {
		for (McpTool tool : project.getRegistry().getAll()) {
			JsonObject json = tool.toJson(true);
			assertThat(json.getAsJsonObject("inputSchema").get("type").getAsString()).isEqualTo("object");
			assertThat(tool.getOutputSchema()).as(tool.getName()).isNotNull();
			assertThat(tool.getDescription()).isNotBlank();
		}
		// write tools hidden when disabled
		assertThat(project.getRegistry().listAvailable()).noneMatch(t -> !t.isReadOnly());
		assertThat(project.getRegistry().getAll()).anyMatch(t -> t.getName().equals("rename_class"));
	}

	@Test
	void projectInfo() {
		JsonObject info = project.call("get_project_info", "{}");
		assertThat(info.get("state").getAsString()).isEqualTo("READY");
		assertThat(info.get("loaded").getAsBoolean()).isTrue();
		assertThat(info.getAsJsonObject("counts").get("classes").getAsInt()).isGreaterThanOrEqualTo(2);
		assertThat(info.get("gui").getAsBoolean()).isFalse();
	}

	@Test
	void listClasses() {
		JsonObject result = project.call("get_all_classes", "{\"package\": \"jadx.mcp.samples\"}");
		List<String> ids = ids(result.getAsJsonArray("classes"));
		assertThat(ids).contains(SERVICE, USER).doesNotContain(HELPER);

		JsonObject withInner = project.call("get_all_classes", "{\"include_inner\": true, \"limit\": 2}");
		assertThat(withInner.get("count").getAsInt()).isEqualTo(2);
		assertThat(withInner.get("has_more").getAsBoolean()).isTrue();
		assertThat(withInner.get("next_offset").getAsInt()).isEqualTo(2);
		JsonObject page2 = project.call("get_all_classes", "{\"include_inner\": true, \"offset\": 2, \"limit\": 100}");
		assertThat(page2.get("total").getAsInt()).isEqualTo(withInner.get("total").getAsInt());
		assertThat(ids(page2.getAsJsonArray("classes"))).contains(HELPER);
	}

	@Test
	void classInfoAndResolveByDifferentNames() {
		JsonObject info = project.call("get_class_info", "{\"class_name\": \"" + HELPER + "\"}");
		assertThat(info.getAsJsonObject("class").get("is_inner").getAsBoolean()).isTrue();
		assertThat(info.get("outer_class_id").getAsString()).isEqualTo(SERVICE);
		// original full name with dot for inner class
		JsonObject byDot = project.call("get_class_info", "{\"class_name\": \"jadx.mcp.samples.SampleService.Helper\"}");
		assertThat(byDot.getAsJsonObject("class").get("id").getAsString()).isEqualTo(HELPER);
		// smali descriptor
		JsonObject byDesc = project.call("get_class_info", "{\"class_name\": \"Ljadx/mcp/samples/SampleUser;\"}");
		assertThat(byDesc.getAsJsonObject("class").get("id").getAsString()).isEqualTo(USER);

		JsonObject err = project.callError("get_class_info", "{\"class_name\": \"jadx.mcp.samples.Missing\"}");
		assertThat(err.get("code").getAsString()).isEqualTo("NOT_FOUND");
	}

	@Test
	void classSource() {
		JsonObject src = project.call("get_class_source", "{\"class_name\": \"" + SERVICE + "\"}");
		String code = src.getAsJsonObject("source").get("text").getAsString();
		assertThat(code).contains("class SampleService", "hello-mcp-", "public static class Helper");
		assertThat(src.getAsJsonObject("source").get("truncated").getAsBoolean()).isFalse();

		// inner class: only inner part of outer class code
		JsonObject inner = project.call("get_class_source", "{\"class_name\": \"" + HELPER + "\"}");
		String innerCode = inner.getAsJsonObject("source").get("text").getAsString();
		assertThat(inner.get("code_class_id").getAsString()).isEqualTo(SERVICE);
		assertThat(innerCode).contains("class Helper", "help(String").doesNotContain("hello-mcp-");
		assertThat(innerCode.trim()).endsWith("}");

		// paging
		JsonObject page = project.call("get_class_source", "{\"class_name\": \"" + SERVICE + "\", \"max_lines\": 3}");
		JsonObject pageSrc = page.getAsJsonObject("source");
		assertThat(pageSrc.get("end_line").getAsInt() - pageSrc.get("start_line").getAsInt()).isEqualTo(2);
		assertThat(pageSrc.get("truncated").getAsBoolean()).isTrue();
		int next = pageSrc.get("next_start_line").getAsInt();
		JsonObject page2 = project.call("get_class_source",
				"{\"class_name\": \"" + SERVICE + "\", \"start_line\": " + next + ", \"max_chars\": 1000}");
		assertThat(page2.getAsJsonObject("source").get("start_line").getAsInt()).isEqualTo(next);
	}

	@Test
	void methodsAndOverloads() {
		JsonObject methods = project.call("get_methods_of_class", "{\"class_name\": \"" + SERVICE + "\"}");
		List<String> ids = ids(methods.getAsJsonArray("methods"));
		assertThat(ids).contains(SERVICE + "->compute(I)I", SERVICE + "->compute(II)I",
				SERVICE + "->compute(Ljava/lang/String;)Ljava/lang/String;");

		JsonObject ambiguous = project.callError("get_method_by_name",
				"{\"class_name\": \"" + SERVICE + "\", \"method_name\": \"compute\"}");
		assertThat(ambiguous.get("code").getAsString()).isEqualTo("AMBIGUOUS");
		assertThat(ambiguous.getAsJsonArray("candidates")).hasSize(3);

		JsonObject byJavaArgs = project.call("get_method_by_name",
				"{\"class_name\": \"" + SERVICE + "\", \"method_name\": \"compute(int, int)\"}");
		JsonObject mth = byJavaArgs.getAsJsonArray("methods").get(0).getAsJsonObject();
		assertThat(mth.getAsJsonObject("method").get("id").getAsString()).isEqualTo(SERVICE + "->compute(II)I");
		assertThat(mth.getAsJsonObject("source").get("text").getAsString()).contains("return a + b;").doesNotContain("hello");

		JsonObject byString = project.call("get_method_by_name",
				"{\"method_name\": \"" + SERVICE + "->compute(String)\"}");
		assertThat(byString.getAsJsonArray("methods").get(0).getAsJsonObject().getAsJsonObject("source").get("text").getAsString())
				.contains("String compute(String s)");

		JsonObject all = project.call("get_method_by_name",
				"{\"class_name\": \"" + SERVICE + "\", \"method_name\": \"compute\", \"all_overloads\": true}");
		assertThat(all.getAsJsonArray("methods")).hasSize(3);

		JsonObject fields = project.call("get_fields_of_class", "{\"class_name\": \"" + SERVICE + "\"}");
		assertThat(ids(fields.getAsJsonArray("fields"))).contains(SERVICE + "->counter:I");
	}

	@Test
	void methodCodePaging() {
		String run = SERVICE + "->run()V";
		JsonObject full = project.call("get_method_by_name", "{\"method_name\": \"" + run + "\"}")
				.getAsJsonArray("methods").get(0).getAsJsonObject();
		int rangeStart = full.get("range_start_line").getAsInt();
		int rangeEnd = full.get("range_end_line").getAsInt();
		JsonObject fullSrc = full.getAsJsonObject("source");
		assertThat(fullSrc.get("start_line").getAsInt()).isEqualTo(rangeStart);
		assertThat(fullSrc.get("end_line").getAsInt()).isEqualTo(rangeEnd);
		assertThat(fullSrc.get("truncated").getAsBoolean()).isFalse();
		assertThat(fullSrc.get("text").getAsString().trim()).startsWith("public void run()").endsWith("}");

		// page lines must describe returned text, not full method range
		JsonObject page = project.call("get_method_by_name", "{\"method_name\": \"" + run + "\", \"max_lines\": 2}")
				.getAsJsonArray("methods").get(0).getAsJsonObject();
		JsonObject pageSrc = page.getAsJsonObject("source");
		assertThat(page.get("range_end_line").getAsInt()).isEqualTo(rangeEnd);
		assertThat(pageSrc.get("start_line").getAsInt()).isEqualTo(rangeStart);
		assertThat(pageSrc.get("end_line").getAsInt()).isEqualTo(rangeStart + 1);
		assertThat(pageSrc.get("truncated").getAsBoolean()).isTrue();
		assertThat(pageSrc.get("text").getAsString().split("\n")).hasSize(2);
		int next = pageSrc.get("next_start_line").getAsInt();
		assertThat(next).isEqualTo(rangeStart + 2);

		// continue inside method range, stops at method end
		JsonObject rest = project.call("get_method_by_name",
				"{\"method_name\": \"" + run + "\", \"start_line\": " + next + "}")
				.getAsJsonArray("methods").get(0).getAsJsonObject().getAsJsonObject("source");
		assertThat(rest.get("start_line").getAsInt()).isEqualTo(next);
		assertThat(rest.get("end_line").getAsInt()).isEqualTo(rangeEnd);
		assertThat(rest.get("truncated").getAsBoolean()).isFalse();
	}

	@Test
	void variables() {
		JsonObject vars = project.call("get_method_variables", "{\"method_name\": \"" + HELPER + "->help(Ljava/lang/String;)I\"}");
		JsonArray arr = vars.getAsJsonArray("variables");
		List<String> names = new ArrayList<>();
		arr.forEach(v -> names.add(v.getAsJsonObject().get("name").getAsString()));
		assertThat(names).contains("text", "length");
	}

	@Test
	void smali() {
		JsonObject smali = project.call("get_smali_of_class", "{\"class_name\": \"" + USER + "\"}");
		assertThat(smali.getAsJsonObject("source").get("text").getAsString()).contains(".method");
	}

	@Test
	void search() {
		JsonObject classes = project.call("search_classes_by_keyword", "{\"keyword\": \"helper\"}");
		assertThat(ids(classes.getAsJsonArray("classes"))).containsExactly(HELPER);

		JsonObject methods = project.call("search_method_by_name", "{\"keyword\": \"help\", \"exact\": true}");
		assertThat(ids(methods.getAsJsonArray("methods"))).containsExactly(HELPER + "->help(Ljava/lang/String;)I");

		JsonObject fields = project.call("search_field_by_name", "{\"keyword\": \"count\"}");
		assertThat(ids(fields.getAsJsonArray("fields"))).contains(SERVICE + "->counter:I");

		JsonObject code = project.call("search_code", "{\"query\": \"hello-mcp\"}");
		assertThat(code.get("complete").getAsBoolean()).isTrue();
		JsonArray matches = code.getAsJsonArray("matches");
		assertThat(matches).hasSize(1);
		JsonObject match = matches.get(0).getAsJsonObject();
		assertThat(match.get("class_id").getAsString()).isEqualTo(SERVICE);
		assertThat(match.getAsJsonObject("node").get("id").getAsString()).isEqualTo(SERVICE + "->run()V");

		// cursor continuation: one result per call
		JsonObject first = project.call("search_code", "{\"query\": \"counter\", \"max_results\": 1}");
		assertThat(first.get("complete").getAsBoolean()).isFalse();
		String cursor = first.get("next_cursor").getAsString();
		JsonObject second = project.call("search_code",
				"{\"query\": \"counter\", \"max_results\": 1, \"cursor\": \"" + cursor + "\"}");
		assertThat(second.getAsJsonArray("matches")).hasSize(1);
		assertThat(second.getAsJsonArray("matches").get(0).getAsJsonObject().get("line").getAsInt())
				.isNotEqualTo(first.getAsJsonArray("matches").get(0).getAsJsonObject().get("line").getAsInt());

		JsonObject badCursor = project.callError("search_code",
				"{\"query\": \"other\", \"cursor\": \"" + cursor + "\"}");
		assertThat(badCursor.get("code").getAsString()).isEqualTo("INVALID_CURSOR");

		JsonObject badRegex = project.callError("search_code", "{\"query\": \"(\", \"regex\": true}");
		assertThat(badRegex.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
	}

	@Test
	void xrefs() {
		JsonObject toHelp = project.call("get_xrefs_to_method", "{\"method_name\": \"" + HELPER + "->help(Ljava/lang/String;)I\"}");
		List<String> users = new ArrayList<>();
		for (JsonElement el : toHelp.getAsJsonArray("usages")) {
			JsonObject usage = el.getAsJsonObject();
			users.add(usage.get("class_id").getAsString());
			assertThat(usage.get("code").getAsString()).contains("help(");
		}
		assertThat(users).contains(SERVICE, USER);
		assertThat(toHelp.get("pagination_unit").getAsString()).isEqualTo("class");
		assertThat(toHelp.get("total_classes").getAsInt()).isEqualTo(toHelp.get("total").getAsInt());
		assertThat(toHelp.get("returned_classes").getAsInt()).isEqualTo(toHelp.get("count").getAsInt());
		assertThat(toHelp.get("returned_usages").getAsInt()).isEqualTo(toHelp.getAsJsonArray("usages").size());

		JsonObject toClass = project.call("get_xrefs_to_class", "{\"class_name\": \"" + SERVICE + "\"}");
		assertThat(toClass.get("total").getAsInt()).isGreaterThanOrEqualTo(1);
		assertThat(toClass.getAsJsonArray("usages").toString()).contains(USER);

		JsonObject toField = project.call("get_xrefs_to_field",
				"{\"class_name\": \"" + SERVICE + "\", \"field_name\": \"counter\", \"include_code\": false}");
		assertThat(toField.getAsJsonArray("usages").toString()).contains("run()V");

		JsonObject calls = project.call("get_method_calls", "{\"method_name\": \"" + USER + "->start()I\"}");
		assertThat(calls.getAsJsonArray("used").toString()).contains("help(Ljava/lang/String;)I");
	}

	@Test
	void catastrophicRegexIsCancellable() {
		jadx.mcp.tools.CancellationToken token = new jadx.mcp.tools.CancellationToken();
		jadx.mcp.tools.ToolCallContext ctx = new jadx.mcp.tools.ToolCallContext(token, "test", 0);
		new Thread(() -> {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// ignore
			}
			token.cancel(jadx.mcp.tools.CancellationToken.Reason.TIMEOUT);
		}).start();
		long start = System.currentTimeMillis();
		JsonObject args = com.google.gson.JsonParser.parseString(
				"{\"query\": \"((x+)+)\\\\2y\", \"regex\": true}").getAsJsonObject();
		JsonObject result = project.getRegistry().call(project.getRegistry().get("search_code"), args, ctx, true);
		assertThat(System.currentTimeMillis() - start).isLessThan(5000);
		assertThat(result.get("isError").getAsBoolean()).isTrue();
		assertThat(result.toString()).contains("TIMEOUT");
	}

	@Test
	void invalidArguments() {
		JsonObject err = project.callError("get_class_source", "{\"class_name\": 5}");
		assertThat(err.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
		JsonObject unknown = project.callError("get_class_source", "{\"class_name\": \"a\", \"bogus\": 1}");
		assertThat(unknown.get("message").getAsString()).contains("bogus");
		JsonObject missing = project.callError("get_class_source", "{}");
		assertThat(missing.get("message").getAsString()).contains("class_name");
	}

	@Test
	void writeToolsDisabled() {
		JsonObject err = project.callError("rename_class", "{\"class_name\": \"" + USER + "\", \"new_name\": \"X\"}");
		assertThat(err.get("code").getAsString()).isEqualTo("WRITE_DISABLED");
	}

	@Test
	void guiToolsWithoutGui() {
		JsonObject err = project.callError("fetch_current_class", "{}");
		assertThat(err.get("code").getAsString()).isEqualTo("UNSUPPORTED");
	}

	@Test
	void resourcesWithoutManifest() {
		JsonObject err = project.callError("get_android_manifest", "{}");
		assertThat(err.get("code").getAsString()).isEqualTo("NOT_FOUND");
		JsonObject files = project.call("get_all_resource_file_names", "{}");
		assertThat(files.get("total").getAsInt()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void schemaValidator() {
		JsonObject schema = jadx.mcp.tools.Schema.object()
				.required("a", jadx.mcp.tools.Schema.integer("a", 1, 10, null))
				.optional("b", jadx.mcp.tools.Schema.enumOf("b", null, "x", "y"))
				.build();
		assertThat(SchemaValidator.validate(schema, com.google.gson.JsonParser.parseString("{\"a\": 5}"))).isEmpty();
		assertThat(SchemaValidator.validate(schema, com.google.gson.JsonParser.parseString("{\"a\": 5.5}"))).hasSize(1);
		assertThat(SchemaValidator.validate(schema, com.google.gson.JsonParser.parseString("{\"a\": 11}"))).hasSize(1);
		assertThat(SchemaValidator.validate(schema, com.google.gson.JsonParser.parseString("{\"a\": 1, \"b\": \"z\"}"))).hasSize(1);
		assertThat(SchemaValidator.validate(schema, com.google.gson.JsonParser.parseString("{}"))).hasSize(1);
	}

	static List<String> ids(JsonArray arr) {
		List<String> list = new ArrayList<>();
		for (JsonElement el : arr) {
			list.add(el.getAsJsonObject().get("id").getAsString());
		}
		return list;
	}
}
