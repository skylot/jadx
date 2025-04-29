package jadx.tests.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceFileContainer;
import jadx.api.ResourceFileContent;
import jadx.api.ResourceType;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.export.ExportGradle;
import jadx.core.export.ExportGradleType;
import jadx.core.export.OutDirs;
import jadx.core.xmlgen.ResContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class ExportGradleTest {
	private static final String MANIFEST_TESTS_DIR = "manifest";

	private final RootNode root = new RootNode(new JadxArgs());

	@TempDir
	private File exportDir;

	protected ICodeInfo loadResource(String filename) {
		return new SimpleCodeInfo(loadResourceContent(MANIFEST_TESTS_DIR, filename));
	}

	private static String loadFileContent(File filePath) {
		try {
			return Files.readString(filePath.toPath());
		} catch (IOException e) {
			fail("Loading file failed", e);
			return "";
		}
	}

	private String loadResourceContent(String dir, String filename) {
		String resPath = dir + '/' + filename;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(resPath)) {
			if (in == null) {
				fail("Resource not found: " + resPath);
				return "";
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			fail("Loading file failed: " + resPath, e);
			return "";
		}
	}

	protected RootNode getRootNode() {
		return root;
	}

	protected void exportGradle(String manifestFilename, String stringsFileName) {
		ResourceFile androidManifest =
				new ResourceFileContent("AndroidManifest.xml", ResourceType.MANIFEST, loadResource(manifestFilename));
		ResContainer strings = ResContainer.textResource(stringsFileName, loadResource(stringsFileName));
		ResContainer arsc = ResContainer.resourceTable("resources.arsc", List.of(strings), new SimpleCodeInfo("empty"));
		ResourceFile arscFile = new ResourceFileContainer("resources.arsc", ResourceType.ARSC, arsc);
		List<ResourceFile> resources = List.of(androidManifest, arscFile);

		root.getArgs().setExportGradleType(ExportGradleType.ANDROID_APP);
		ExportGradle export = new ExportGradle(root, exportDir, resources);
		OutDirs outDirs = export.init();
		assertThat(outDirs.getSrcOutDir()).exists();
		assertThat(outDirs.getResOutDir()).exists();
		export.generateGradleFiles();
	}

	protected String getAppGradleBuild() {
		return loadFileContent(new File(exportDir, "app/build.gradle"));
	}

	protected String getSettingsGradle() {
		return loadFileContent(new File(exportDir, "settings.gradle"));
	}

	protected File getGradleProperiesFile() {
		return new File(exportDir, "gradle.properties");
	}

	protected String getGradleProperties() {
		return loadFileContent(getGradleProperiesFile());
	}
}
