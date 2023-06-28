package jadx.tests.export;

import org.junit.jupiter.api.Test;

import jadx.core.export.GradleInfoStorage;
import jadx.tests.api.ExportGradleTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestApacheHttpClient extends ExportGradleTest {

	@Test
	void test() {
		GradleInfoStorage gradleInfo = getRootNode().getGradleInfoStorage();
		gradleInfo.setUseApacheHttpLegacy(true);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		assertThat(getAppGradleBuild()).contains("        useLibrary 'org.apache.http.legacy'");

		gradleInfo.setUseApacheHttpLegacy(false);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		assertThat(getAppGradleBuild()).doesNotContain("        useLibrary 'org.apache.http.legacy'");
	}
}
