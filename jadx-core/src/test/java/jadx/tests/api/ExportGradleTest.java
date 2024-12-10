package jadx.tests.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceFileContent;
import jadx.api.ResourceType;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.export.ExportGradleProject;
import jadx.core.export.ExportGradleTask;
import jadx.core.xmlgen.ResContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class ExportGradleTest {
	private static final String MANIFEST_TESTS_DIR = "src/test/manifest";

	private final RootNode root = new RootNode(new JadxArgs());

	@TempDir
	private File exportDir;

	protected ICodeInfo loadResource(String filename) {
		return new SimpleCodeInfo(loadFileContent(new File(MANIFEST_TESTS_DIR, filename)));
	}

	private static String loadFileContent(File filePath) {
		try {
			return Files.readString(filePath.toPath());
		} catch (IOException e) {
			fail("Loading file failed", e);
			return "";
		}
	}

	protected RootNode getRootNode() {
		return root;
	}

	protected void exportGradle(String manifestFilename, String stringsFileName) {
		ResourceFile androidManifest = new ResourceFileContent(manifestFilename,
				ResourceType.XML, loadResource(manifestFilename));
		ResContainer strings = ResContainer.textResource(stringsFileName, loadResource(stringsFileName));

		ExportGradleTask exportGradleTask = new ExportGradleTask(List.of(androidManifest), root, exportDir);
		exportGradleTask.init();
		assertThat(exportGradleTask.getSrcOutDir()).exists();
		assertThat(exportGradleTask.getResOutDir()).exists();

		ExportGradleProject export = new ExportGradleProject(root, exportDir, androidManifest, strings);
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

	protected String getGradleProperies() {
		return loadFileContent(getGradleProperiesFile());
	}
}
