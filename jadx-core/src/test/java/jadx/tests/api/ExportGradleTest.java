package jadx.tests.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JadxDecompilerTestUtils;
import jadx.api.ResourceFile;
import jadx.core.dex.nodes.RootNode;
import jadx.core.export.ExportGradleProject;
import jadx.core.xmlgen.ResContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ExportGradleTest {

	private static final String MANIFEST_TESTS_DIR = "src/test/manifest";

	@TempDir
	private File exportDir;

	protected ResContainer createResourceContainer(String filename) {
		final ResContainer container = mock(ResContainer.class);
		ICodeInfo codeInfo = mock(ICodeInfo.class);
		when(codeInfo.getCodeStr()).thenReturn(loadFileContent(new File(MANIFEST_TESTS_DIR, filename)));
		when(container.getText()).thenReturn(codeInfo);
		return container;
	}

	private static String loadFileContent(File filePath) {
		StringBuilder contentBuilder = new StringBuilder();

		try (Stream<String> stream = Files.lines(filePath.toPath(), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			fail("Loading file failed: %s", e.getMessage());
		}
		return contentBuilder.toString();
	}

	protected void exportGradle(String manifestFilename, String stringsFileName) {
		final JadxDecompiler decompiler = JadxDecompilerTestUtils.getMockDecompiler();
		ResourceFile androidManifest = mock(ResourceFile.class);
		final ResContainer androidManifestContainer = createResourceContainer(manifestFilename);
		when(androidManifest.loadContent()).thenReturn(androidManifestContainer);
		final ResContainer strings = createResourceContainer(stringsFileName);
		final RootNode root = decompiler.getRoot();

		final ExportGradleProject export =
				new ExportGradleProject(root, exportDir, androidManifest, strings);
		export.init();
		assertThat(export.getSrcOutDir().exists());
		assertThat(export.getResOutDir().exists());
	}

	protected String getAppGradleBuild() {
		File appBuildGradle = new File(exportDir, "app/build.gradle");
		assertThat(appBuildGradle.exists());
		return loadFileContent(appBuildGradle);
	}

	protected String getSettingsGradle() {
		File settingsGradle = new File(exportDir, "settings.gradle");
		assertThat(settingsGradle.exists());
		return loadFileContent(settingsGradle);
	}
}
