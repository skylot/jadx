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
	void applyFromConfigSetsLocaleBeforeFullParse() throws Exception {
		Path configPath = tempDir.resolve("gui.json");
		Files.writeString(configPath,
				"{\n"
						+ "  \"langLocale\": {\n"
						+ "    \"locale\": {\n"
						+ "      \"language\": \"zh\",\n"
						+ "      \"country\": \"CN\"\n"
						+ "    }\n"
						+ "  },\n"
						+ "  \"shortcuts\": {\n"
						+ "    \"OPEN\": {\n"
						+ "      \"key\": 79,\n"
						+ "      \"modifiers\": 128\n"
						+ "    }\n"
						+ "  }\n"
						+ "}\n");

		GuiConfigLocale.applyFromConfig(configPath);

		assertThat(NLS.currentLocale()).isEqualTo(new LangLocale("zh", "CN"));
	}
}
