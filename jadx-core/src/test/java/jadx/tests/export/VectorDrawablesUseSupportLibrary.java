package jadx.tests.export;

import org.junit.jupiter.api.Test;

import jadx.core.export.GradleInfoStorage;
import jadx.tests.api.ExportGradleTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class VectorDrawablesUseSupportLibrary extends ExportGradleTest {

	@Test
	void test() {
		GradleInfoStorage gradleInfo = getRootNode().getGradleInfoStorage();
		gradleInfo.setVectorFillType(true);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		assertThat(getAppGradleBuild()).contains("        vectorDrawables.useSupportLibrary = true");

		gradleInfo.setVectorFillType(false);
		gradleInfo.setVectorPathData(true);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		assertThat(getAppGradleBuild()).contains("        vectorDrawables.useSupportLibrary = true");

		exportGradle("MinSdkVersion25.xml", "strings.xml");
		assertThat(getAppGradleBuild()).doesNotContain("        vectorDrawables.useSupportLibrary = true");

	}
}
