package jadx.tests.export;

import org.junit.jupiter.api.Test;

import jadx.tests.api.ExportGradleTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class IllegalCharsForGradleWrapper extends ExportGradleTest {

	@Test
	void test() {
		exportGradle("IllegalCharsForGradleWrapper.xml", "strings.xml");

		assertThat(getSettingsGradle()).contains("'JadxTestApp'");
	}
}
