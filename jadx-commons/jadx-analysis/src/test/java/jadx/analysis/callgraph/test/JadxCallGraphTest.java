package jadx.analysis.callgraph.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.analysis.callgraph.CallGraphExportDot;
import jadx.analysis.callgraph.CallGraphExportJson;
import jadx.analysis.callgraph.JadxCallGraph;
import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphEdge;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;

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
