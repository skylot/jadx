package jadx.analysis.callgraph.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.analysis.callgraph.CallGraphExportDot;
import jadx.analysis.callgraph.CallGraphExportJson;
import jadx.analysis.callgraph.JadxCallGraph;
import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.core.dex.nodes.MethodNode;

import static org.assertj.core.api.Assertions.assertThat;

class JadxCallGraphTest {
	@TempDir
	Path tempDir;

	@SuppressWarnings("unused")
	void usageExample() {
		JadxArgs args = new JadxArgs();
		args.addInputFile(new File("input.apk"));
		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();

			ICallGraph callGraph = JadxCallGraph.builder(jadx)
					.includePackages("com.example")
					.resolvedOnly(false)
					.build();

			for (ICallGraphEdge edge : callGraph.edges()) {
				if (edge.isResolved()) {
					System.out.printf("Edge from '%s' to '%s'%n", edge.from(), edge.to());
				}
			}
			callGraph.writeDot(Path.of("test.dot"));
			callGraph.writeJson(Path.of("test.json"));
		}
	}

	@Test
	void simpleTest() {
		JadxArgs args = new JadxArgs();
		args.addInputFile(getSampleFile("simple.smali"));
		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();

			ICallGraph callGraph = JadxCallGraph.builder(jadx)
					.includePackages("test.pkg")
					.resolvedOnly(false)
					.build();

			assertThat(callGraph.edges()).hasSize(1);

			for (ICallGraphEdge edge : callGraph.edges()) {
				System.out.println("Edge from " + edge.from() + " to " + edge.to());
			}

			String dotStr = new CallGraphExportDot(jadx.getArgs(), callGraph).writeToString();
			System.out.println("dot: " + dotStr);

			String jsonStr = new CallGraphExportJson(callGraph).writeToString();
			System.out.println("json: " + jsonStr);

			callGraph.writeDot(tempDir.resolve("test.dot"));
			callGraph.writeJson(tempDir.resolve("test.json"));
		}
	}

	@Test
	void multiPrefixIncludeAndExcludePackagesTest() throws IOException {
		Path entrySmali = tempDir.resolve("Entry.smali");
		Files.writeString(entrySmali, ""
				+ ".class public Lapp/core/Entry;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static run()V\n"
				+ "    .registers 2\n"
				+ "    invoke-static {}, Lapp/feature/Worker;->work()V\n"
				+ "    invoke-static {}, Lvendor/sdk/Tracker;->track()V\n"
				+ "    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n"
				+ "    const-string v1, \"hi\"\n"
				+ "    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n"
				+ "    return-void\n"
				+ ".end method\n"
				+ ".method public static helper()V\n"
				+ "    .registers 0\n"
				+ "    return-void\n"
				+ ".end method\n");

		Path workerSmali = tempDir.resolve("Worker.smali");
		Files.writeString(workerSmali, ""
				+ ".class public Lapp/feature/Worker;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static work()V\n"
				+ "    .registers 0\n"
				+ "    invoke-static {}, Lapp/core/Entry;->helper()V\n"
				+ "    return-void\n"
				+ ".end method\n");

		Path trackerSmali = tempDir.resolve("Tracker.smali");
		Files.writeString(trackerSmali, ""
				+ ".class public Lvendor/sdk/Tracker;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static track()V\n"
				+ "    .registers 2\n"
				+ "    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n"
				+ "    const-string v1, \"track\"\n"
				+ "    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n"
				+ "    return-void\n"
				+ ".end method\n");

		JadxArgs args = new JadxArgs();
		args.addInputFile(entrySmali.toFile());
		args.addInputFile(workerSmali.toFile());
		args.addInputFile(trackerSmali.toFile());

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();

			// Multi-prefix include via Collection<String>
			ICallGraph includeMulti = JadxCallGraph.builder(jadx)
					.includePackages(List.of("app.core", "app.feature"))
					.resolvedOnly(true)
					.build();
			assertThat(edgePairs(includeMulti)).containsExactlyInAnyOrder(
					"app.core.Entry.run()V -> app.feature.Worker.work()V",
					"app.feature.Worker.work()V -> app.core.Entry.helper()V");

			// Additive chaining with single-string includePackages + normalization (trailing '.' and '.*')
			ICallGraph includeChained = JadxCallGraph.builder(jadx)
					.includePackages(" app.core. ")
					.includePackages("app.feature.*")
					.includePackages("   ")
					.resolvedOnly(true)
					.build();
			assertThat(edgePairs(includeChained)).containsExactlyInAnyOrderElementsOf(edgePairs(includeMulti));

			// Include app.core + java.io with unresolved edges
			ICallGraph includeWithUnresolved = JadxCallGraph.builder(jadx)
					.includePackages(Arrays.asList("app.core", "java.io", null, ""))
					.resolvedOnly(false)
					.build();
			assertThat(edgePairs(includeWithUnresolved)).containsExactly(
					"app.core.Entry.run()V -> java.io.PrintStream.println(Ljava/lang/String;)V");
			assertThat(includeWithUnresolved.edges().get(0).isResolved()).isFalse();

			// Exclude vendor.sdk and java.io
			ICallGraph excluded = JadxCallGraph.builder(jadx)
					.excludePackages(List.of("vendor.sdk", "java.io"))
					.resolvedOnly(false)
					.build();
			assertThat(edgePairs(excluded)).containsExactlyInAnyOrder(
					"app.core.Entry.run()V -> app.feature.Worker.work()V",
					"app.feature.Worker.work()V -> app.core.Entry.helper()V");

			// Combine includePackages("app") + excludePackages("app.feature")
			ICallGraph includeMinusSubpkg = JadxCallGraph.builder(jadx)
					.includePackages("app")
					.excludePackages("app.feature")
					.resolvedOnly(true)
					.build();
			assertThat(includeMinusSubpkg.edges()).isEmpty();
		}
	}

	@Test
	void useAliasTest() throws IOException {
		Path obfSmali = tempDir.resolve("Obf.smali");
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

		JadxArgs args = new JadxArgs();
		args.addInputFile(obfSmali.toFile());
		args.setDeobfuscationOn(true);
		args.setDeobfuscationMinLength(3);
		args.setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode.IGNORE);

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();

			ICallGraph rawGraph = JadxCallGraph.builder(jadx)
					.resolvedOnly(true)
					.build();
			assertThat(rawGraph.isUseAlias()).isFalse();
			assertThat(rawGraph.edges()).hasSize(1);

			ICallGraphEdge edge = rawGraph.edges().get(0);
			MethodNode fromMth = edge.from().getMethodNode();
			MethodNode toMth = edge.to().getMethodNode();
			assertThat(fromMth).isNotNull();
			assertThat(toMth).isNotNull();

			String expectedFromAlias = fromMth.getMethodInfo().getDeclClass().getAliasFullName()
					+ '.' + fromMth.getMethodInfo().makeSignature(true, true);
			String expectedToAlias = toMth.getMethodInfo().getDeclClass().getAliasFullName()
					+ '.' + toMth.getMethodInfo().makeSignature(true, true);
			assertThat(expectedFromAlias).isNotEqualTo("a.b.a()V");
			assertThat(expectedToAlias).isNotEqualTo("a.b.b()V");

			// Default (raw IDs)
			String rawJson = new CallGraphExportJson(rawGraph).writeToString();
			String rawDot = new CallGraphExportDot(jadx.getArgs(), rawGraph).writeToString();
			assertThat(rawJson).contains("a.b.a()V").contains("a.b.b()V");
			assertThat(rawDot).contains("a.b.a()V").contains("a.b.b()V");

			// Builder useAlias(true)
			ICallGraph aliasGraph = JadxCallGraph.builder(jadx)
					.resolvedOnly(true)
					.useAlias(true)
					.build();
			assertThat(aliasGraph.isUseAlias()).isTrue();
			String aliasJson = new CallGraphExportJson(aliasGraph).writeToString();
			String aliasDot = new CallGraphExportDot(jadx.getArgs(), aliasGraph).writeToString();
			assertThat(aliasJson).contains(expectedFromAlias).contains(expectedToAlias).doesNotContain("a.b.a()V");
			assertThat(aliasDot).contains(expectedFromAlias).contains(expectedToAlias).doesNotContain("a.b.a()V");

			// Exporter constructor override on rawGraph
			String overrideJson = new CallGraphExportJson(rawGraph, true).writeToString();
			String overrideDot = new CallGraphExportDot(jadx.getArgs(), rawGraph, true).writeToString();
			assertThat(overrideJson).isEqualTo(aliasJson);
			assertThat(overrideDot).isEqualTo(aliasDot);
		}
	}

	private static List<String> edgePairs(ICallGraph callGraph) {
		return callGraph.edges().stream()
				.map(e -> e.from().getMethodInfo().getRawFullId() + " -> " + e.to().getMethodInfo().getRawFullId())
				.collect(Collectors.toList());
	}

	private File getSampleFile(String sampleName) {
		try {
			URL resource = getClass().getResource("/samples/" + sampleName);
			assertThat(resource).describedAs("Sample not found: %s", sampleName).isNotNull();
			return new File(resource.toURI().toURL().getFile());
		} catch (MalformedURLException | URISyntaxException e) {
			throw new RuntimeException("Failed to load sample file: " + sampleName, e);
		}
	}
}
