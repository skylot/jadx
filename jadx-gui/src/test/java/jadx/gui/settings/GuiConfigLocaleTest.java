package jadx.gui.settings;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

import static org.assertj.core.api.Assertions.assertThat;

class GuiConfigLocaleTest {

	@TempDir
	Path tempDir;

	@Test
	void applyFromConfigSetsLocale() throws Exception {
		LangLocale previous = NLS.currentLocale();
		Path configPath = tempDir.resolve("gui.json");
		Files.writeString(configPath,
				"{\"langLocale\":{\"locale\":{\"language\":\"zh\",\"country\":\"CN\"}}}\n");
		try {
			GuiConfigLocale.applyFromConfig(configPath);
			assertThat(NLS.currentLocale()).isEqualTo(new LangLocale("zh", "CN"));
		} finally {
			NLS.setLocale(previous != null ? previous : NLS.defaultLocale());
		}
	}
}
