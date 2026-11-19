package jadx.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCallGraphCli extends BaseCliIntegrationTest {

	@Test
	public void testCallGraphDefaultResolvedOnly() throws IOException {
		Path entrySmali = writeMultiPkgSample();
		int result = execJadxCli(new String[] {
				"--no-src", "--no-res",
				"--call-graph", "json",
				"-d", outputDir.toAbsolutePath().toString(),
				entrySmali.toAbsolutePath().toString()
		});
		assertThat(result).isEqualTo(0);

		String json = Files.readString(outputDir.resolve("callgraph.json"));
		assertThat(json).contains("app.core.Entry.run()V");
		assertThat(json).contains("app.core.Entry.helper()V");
		assertThat(json).doesNotContain("java.io.PrintStream.println");
	}

	@Test
	public void testCallGraphUnresolvedFlag() throws IOException {
		Path entrySmali = writeMultiPkgSample();
		int result = execJadxCli(new String[] {
				"--no-src", "--no-res",
				"--call-graph", "json",
				"--call-graph-unresolved",
				"-d", outputDir.toAbsolutePath().toString(),
				entrySmali.toAbsolutePath().toString()
		});
		assertThat(result).isEqualTo(0);

		String json = Files.readString(outputDir.resolve("callgraph.json"));
		assertThat(json).contains("app.core.Entry.run()V");
		assertThat(json).contains("java.io.PrintStream.println(Ljava/lang/String;)V");
		assertThat(json).contains("\"resolved\":false");
	}

	@Test
	public void testCallGraphIncludeAndExcludePackages() throws IOException {
		Path entrySmali = writeMultiPkgSample();
		Path vendorSmali = testDir.resolve("Tracker.smali");
		Files.writeString(vendorSmali, ""
				+ ".class public Lvendor/sdk/Tracker;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static track()V\n"
				+ "    .registers 0\n"
				+ "    invoke-static {}, Lapp/core/Entry;->helper()V\n"
				+ "    return-void\n"
				+ ".end method\n");

		int result = execJadxCli(new String[] {
				"--no-src", "--no-res",
				"--call-graph", "json",
				"--call-graph-unresolved",
				"--call-graph-include-packages", "app.core, vendor.sdk",
				"--call-graph-exclude-packages", "vendor.sdk",
				"-d", outputDir.toAbsolutePath().toString(),
				entrySmali.toAbsolutePath().toString(),
				vendorSmali.toAbsolutePath().toString()
		});
		assertThat(result).isEqualTo(0);

		String json = Files.readString(outputDir.resolve("callgraph.json"));
		assertThat(json).contains("app.core.Entry.run()V");
		assertThat(json).contains("app.core.Entry.helper()V");
		assertThat(json).doesNotContain("vendor.sdk.Tracker");
		assertThat(json).doesNotContain("java.io.PrintStream");
	}

	@Test
	public void testCallGraphUseAlias() throws IOException {
		Path obfSmali = testDir.resolve("Obf.smali");
		Files.writeString(obfSmali, ""
				+ ".class public La/b;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static a()V\n"
				+ "    .registers 0\n"
				+ "    invoke-static {}, La/b;->b()V\n"
				+ "    return-void\n"
				+ ".end method\n"
				+ ".method public static b()V\n"
				+ "    .registers 0\n"
				+ "    return-void\n"
				+ ".end method\n");

		int resultRaw = execJadxCli(new String[] {
				"--no-src", "--no-res",
				"--deobf",
				"--deobf-min", "3",
				"--deobf-cfg-file-mode", "ignore",
				"--call-graph", "dot",
				"-d", outputDir.toAbsolutePath().toString(),
				obfSmali.toAbsolutePath().toString()
		});
		assertThat(resultRaw).isEqualTo(0);
		String rawDot = Files.readString(outputDir.resolve("callgraph.dot"));
		assertThat(rawDot).contains("a.b.a()V").contains("a.b.b()V");

		Path aliasOutDir = testDir.resolve("output-alias");
		int resultAlias = execJadxCli(new String[] {
				"--no-src", "--no-res",
				"--deobf",
				"--deobf-min", "3",
				"--deobf-cfg-file-mode", "ignore",
				"--call-graph", "dot",
				"--call-graph-use-alias",
				"-d", aliasOutDir.toAbsolutePath().toString(),
				obfSmali.toAbsolutePath().toString()
		});
		assertThat(resultAlias).isEqualTo(0);
		String aliasDot = Files.readString(aliasOutDir.resolve("callgraph.dot"));
		assertThat(aliasDot).doesNotContain("a.b.a()V").doesNotContain("a.b.b()V");
		assertThat(aliasDot).contains("N1 -> N2;");
	}

	private Path writeMultiPkgSample() throws IOException {
		Path entrySmali = testDir.resolve("Entry.smali");
		Files.writeString(entrySmali, ""
				+ ".class public Lapp/core/Entry;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static run()V\n"
				+ "    .registers 2\n"
				+ "    invoke-static {}, Lapp/core/Entry;->helper()V\n"
				+ "    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n"
				+ "    const-string v1, \"hi\"\n"
				+ "    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n"
				+ "    return-void\n"
				+ ".end method\n"
				+ ".method public static helper()V\n"
				+ "    .registers 0\n"
				+ "    return-void\n"
				+ ".end method\n");
		return entrySmali;
	}
}
