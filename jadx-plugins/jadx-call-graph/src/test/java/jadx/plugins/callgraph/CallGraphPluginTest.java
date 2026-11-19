package jadx.plugins.callgraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;

import static org.assertj.core.api.Assertions.assertThat;

class CallGraphPluginTest {

	@Test
	void testOptionsParsing() {
		CallGraphOptions options = new CallGraphOptions();
		options.setOptions(Map.of());
		assertThat(options.getFormat()).isEqualTo(CallGraphOptions.Format.NONE);
		assertThat(options.isUnresolved()).isFalse();
		assertThat(options.getIncludePackages()).isEmpty();
		assertThat(options.getExcludePackages()).isEmpty();
		assertThat(options.isUseAlias()).isFalse();

		options.setOptions(Map.of(
				CallGraphOptions.FORMAT_OPT, "json",
				CallGraphOptions.UNRESOLVED_OPT, "true",
				CallGraphOptions.INCLUDE_PACKAGES_OPT, "com.example,com.other",
				CallGraphOptions.EXCLUDE_PACKAGES_OPT, "com.vendor",
				CallGraphOptions.USE_ALIAS_OPT, "true"));
		assertThat(options.getFormat()).isEqualTo(CallGraphOptions.Format.JSON);
		assertThat(options.isUnresolved()).isTrue();
		assertThat(options.getIncludePackages()).isEqualTo("com.example,com.other");
		assertThat(options.getExcludePackages()).isEqualTo("com.vendor");
		assertThat(options.isUseAlias()).isTrue();
	}

	@Test
	void testPluginAfterLoadPassExportsJson(@TempDir Path tempDir) throws Exception {
		Path smaliFile = tempDir.resolve("Sample.smali");
		Files.writeString(smaliFile, ""
				+ ".class public Lapp/Sample;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static main()V\n"
				+ "    .registers 0\n"
				+ "    invoke-static {}, Lapp/Sample;->helper()V\n"
				+ "    return-void\n"
				+ ".end method\n"
				+ ".method public static helper()V\n"
				+ "    .registers 0\n"
				+ "    return-void\n"
				+ ".end method\n");

		Path outDir = tempDir.resolve("out");
		JadxArgs args = new JadxArgs();
		args.setInputFile(smaliFile.toFile());
		args.setOutDir(outDir.toFile());
		args.setPluginOptions(Map.of(CallGraphOptions.FORMAT_OPT, "json"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
		}

		Path jsonFile = outDir.resolve("callgraph.json");
		assertThat(jsonFile).isRegularFile();
		String json = Files.readString(jsonFile);
		assertThat(json).contains("app.Sample.main()V");
		assertThat(json).contains("app.Sample.helper()V");
	}
}
