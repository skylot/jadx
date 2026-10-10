package jadx.gui.settings.ui.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static jadx.gui.settings.ui.plugins.InstallPluginDialog.checkLocationId;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallPluginDialogTest {

	@TempDir
	Path tempDir;

	@Test
	public void testLocationIdKept() {
		assertThat(checkLocationId("github:skylot:jadx")).isEqualTo("github:skylot:jadx");
		assertThat(checkLocationId("file:/plugins/plugin.jar")).isEqualTo("file:/plugins/plugin.jar");
	}

	@Test
	public void testPlainFilePath() throws IOException {
		Path jar = Files.createFile(tempDir.resolve("plugin.jar"));
		String expected = "file:" + jar.toAbsolutePath();
		assertThat(checkLocationId(jar.toString())).isEqualTo(expected);
		assertThat(checkLocationId("  " + jar + " \n")).isEqualTo(expected);
		assertThat(checkLocationId("\"" + jar + "\"")).isEqualTo(expected);
	}

	@Test
	public void testInvalidLocation() {
		assertThat(checkLocationId("")).isNull();
		assertThat(checkLocationId("\"")).isNull();
		assertThat(checkLocationId("unknown:plugin")).isNull();
		assertThat(checkLocationId(tempDir.resolve("missing.jar").toString())).isNull();
		assertThat(checkLocationId(tempDir.toString())).isNull();
	}
}
