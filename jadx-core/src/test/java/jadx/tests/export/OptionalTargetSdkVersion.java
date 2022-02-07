package jadx.tests.export;

import org.junit.jupiter.api.Test;

import jadx.tests.api.ExportGradleTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class OptionalTargetSdkVersion extends ExportGradleTest {

	@Test
	void test() {
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");

		assertThat(getAppGradleBuild()).contains("targetSdkVersion 14");
	}

}
