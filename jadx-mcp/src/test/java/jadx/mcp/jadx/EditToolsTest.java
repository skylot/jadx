package jadx.mcp.jadx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

class EditToolsTest {
	private static final String SERVICE = "jadx.mcp.samples.SampleService";
	private static final String HELPER = "jadx.mcp.samples.SampleService$Helper";
	private static final String USER = "jadx.mcp.samples.SampleUser";

	private SampleProject project;

	@BeforeEach
	void init() {
		project = new SampleProject(true);
	}

	@AfterEach
	void close() {
		project.close();
	}

	private String source(String cls) {
		return project.call("get_class_source", "{\"class_name\": \"" + cls + "\"}")
				.getAsJsonObject("source").get("text").getAsString();
	}

	@Test
	void renameClassAndReset() {
		JsonObject result = project.call("rename_class", "{\"class_name\": \"" + HELPER + "\", \"new_name\": \"TextHelper\"}");
		assertThat(result.get("success").getAsBoolean()).isTrue();
		assertThat(result.get("new_name").getAsString()).isEqualTo("jadx.mcp.samples.SampleService.TextHelper");
		assertThat(result.getAsJsonArray("updated_classes").toString()).contains(SERVICE, USER);
		assertThat(result.getAsJsonObject("save").get("persisted").getAsBoolean()).isFalse();
		// id stable, usages updated
		assertThat(result.getAsJsonObject("node").get("id").getAsString()).isEqualTo(HELPER);
		assertThat(source(USER)).contains("SampleService.TextHelper helper").doesNotContain("SampleService.Helper helper");
		// resolve by new name
		JsonObject info = project.call("get_class_info", "{\"class_name\": \"jadx.mcp.samples.SampleService.TextHelper\"}");
		assertThat(info.getAsJsonObject("class").get("id").getAsString()).isEqualTo(HELPER);
		assertThat(info.getAsJsonObject("class").get("renamed").getAsBoolean()).isTrue();

		JsonObject reset = project.call("rename_class", "{\"class_name\": \"" + HELPER + "\", \"reset\": true}");
		assertThat(reset.get("new_name").getAsString()).isEqualTo("jadx.mcp.samples.SampleService.Helper");
		assertThat(source(USER)).doesNotContain("TextHelper");
	}

	@Test
	void renameValidation() {
		JsonObject invalid = project.callError("rename_class", "{\"class_name\": \"" + HELPER + "\", \"new_name\": \"a.b.C\"}");
		assertThat(invalid.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
		JsonObject keyword = project.callError("rename_field",
				"{\"field_name\": \"" + SERVICE + "->counter:I\", \"new_name\": \"class\"}");
		assertThat(keyword.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
		JsonObject ctor = project.callError("rename_method",
				"{\"method_name\": \"" + SERVICE + "-><init>()V\", \"new_name\": \"build\"}");
		assertThat(ctor.get("code").getAsString()).isEqualTo("UNSUPPORTED");
		JsonObject missingName = project.callError("rename_method", "{\"method_name\": \"" + SERVICE + "->run()V\"}");
		assertThat(missingName.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
	}

	@Test
	void renameMembers() {
		JsonObject mth = project.call("rename_method",
				"{\"class_name\": \"" + SERVICE + "\", \"method_name\": \"compute(int, int)\", \"new_name\": \"sum\"}");
		assertThat(mth.get("success").getAsBoolean()).isTrue();
		assertThat(mth.get("old_name").getAsString()).isEqualTo("compute");
		assertThat(source(SERVICE)).contains("public int sum(int a, int b)", "public int compute(int a)");

		JsonObject fld = project.call("rename_field",
				"{\"class_name\": \"" + SERVICE + "\", \"field_name\": \"counter\", \"new_name\": \"total\"}");
		assertThat(fld.get("new_name").getAsString()).isEqualTo("total");
		assertThat(source(SERVICE)).contains("this.total").doesNotContain("this.counter");

		JsonObject var = project.call("rename_variable",
				"{\"method_name\": \"" + HELPER + "->help(Ljava/lang/String;)I\", \"variable\": \"length\", \"new_name\": \"len\"}");
		assertThat(var.get("success").getAsBoolean()).isTrue();
		assertThat(var.get("new_name").getAsString()).isEqualTo("len");
		assertThat(source(HELPER)).contains("int len = text.length();");
	}

	@Test
	void renamePackage() {
		JsonObject pkg = project.call("rename_package",
				"{\"package_name\": \"jadx.mcp.samples\", \"new_name\": \"com.example.app\"}");
		assertThat(pkg.get("success").getAsBoolean()).isTrue();
		JsonObject info = project.call("get_class_info", "{\"class_name\": \"" + USER + "\"}");
		assertThat(info.getAsJsonObject("class").get("name").getAsString()).isEqualTo("com.example.app.SampleUser");
		assertThat(source(USER)).contains("package com.example.app;");
	}

	@Test
	void comments() {
		JsonObject added = project.call("set_comment",
				"{\"node_id\": \"" + SERVICE + "->run()V\", \"comment\": \"Entry point of service\"}");
		assertThat(added.get("action").getAsString()).isEqualTo("added");
		assertThat(source(SERVICE)).contains("// Entry point of service");

		JsonObject replaced = project.call("set_comment",
				"{\"node_id\": \"" + SERVICE + "->run()V\", \"comment\": \"Main entry\", \"style\": \"JAVADOC\"}");
		assertThat(replaced.get("action").getAsString()).isEqualTo("replaced");
		String code = source(SERVICE);
		assertThat(code).contains("Main entry").doesNotContain("Entry point of service");

		// line comment inside method: find line with message
		JsonObject search = project.call("search_code", "{\"query\": \"hello-mcp\"}");
		int line = search.getAsJsonArray("matches").get(0).getAsJsonObject().get("line").getAsInt();
		JsonObject lineComment = project.call("set_comment",
				"{\"node_id\": \"" + SERVICE + "\", \"line\": " + line + ", \"comment\": \"build message\"}");
		assertThat(lineComment.getAsJsonObject("target").get("code_ref").getAsString()).startsWith("INSN:");
		assertThat(source(SERVICE)).contains("// build message");

		JsonObject list = project.call("get_comments", "{\"class_name\": \"" + SERVICE + "\"}");
		assertThat(list.get("total").getAsInt()).isEqualTo(2);

		JsonObject removed = project.call("remove_comment", "{\"node_id\": \"" + SERVICE + "->run()V\"}");
		assertThat(removed.get("action").getAsString()).isEqualTo("removed");
		assertThat(source(SERVICE)).doesNotContain("Main entry");

		JsonObject notFound = project.callError("remove_comment", "{\"node_id\": \"" + SERVICE + "->run()V\"}");
		assertThat(notFound.get("code").getAsString()).isEqualTo("NOT_FOUND");

		JsonObject badLine = project.callError("set_comment",
				"{\"node_id\": \"" + SERVICE + "\", \"line\": 100000, \"comment\": \"x\"}");
		assertThat(badLine.get("code").getAsString()).isEqualTo("INVALID_ARGUMENT");
	}

	@Test
	void saveHeadless() {
		JsonObject save = project.call("save_project", "{}");
		assertThat(save.getAsJsonObject("save").get("persisted").getAsBoolean()).isFalse();
	}
}
