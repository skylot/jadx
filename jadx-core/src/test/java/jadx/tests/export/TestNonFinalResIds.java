package jadx.tests.export;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadx.core.export.GradleInfoStorage;
import jadx.tests.api.ExportGradleTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNonFinalResIds extends ExportGradleTest {

	@Test
	void test() {
		GradleInfoStorage gradleInfo = getRootNode().getGradleInfoStorage();
		gradleInfo.setNonFinalResIds(false);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		Assertions.assertFalse(getGradleProperiesFile().exists());

		gradleInfo.setNonFinalResIds(true);
		exportGradle("OptionalTargetSdkVersion.xml", "strings.xml");
		assertThat(getGradleProperties()).containsOne("android.nonFinalResIds=false");
	}
}
