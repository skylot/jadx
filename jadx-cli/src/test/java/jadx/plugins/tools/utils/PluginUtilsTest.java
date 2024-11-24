package jadx.plugins.tools.utils;

import org.junit.jupiter.api.Test;

import static jadx.plugins.tools.utils.PluginUtils.extractVersion;
import static org.assertj.core.api.Assertions.assertThat;

class PluginUtilsTest {

	@Test
	public void testExtractVersion() {
		assertThat(extractVersion("plugin-name-v1.2.3.jar")).isEqualTo("1.2.3");
		assertThat(extractVersion("plugin-name-v1.2.jar")).isEqualTo("1.2");
		assertThat(extractVersion("1.2.3.jar")).isEqualTo("1.2.3");
	}

}
