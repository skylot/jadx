package jadx.tests.functional;

import org.junit.jupiter.api.Test;

import jadx.core.export.TemplateFile;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TemplateFileTest {

	@Test
	public void testBuildGradle() throws Exception {
		TemplateFile tmpl = TemplateFile.fromResources("/export/android/app.build.gradle.tmpl");
		tmpl.add("applicationId", "SOME_ID");
		tmpl.add("minSdkVersion", 1);
		tmpl.add("targetSdkVersion", 2);
		tmpl.add("versionCode", 3);
		tmpl.add("versionName", "1.2.3");
		tmpl.add("additionalOptions", "useLibrary 'org.apache.http.legacy'");
		String res = tmpl.build();
		System.out.println(res);

		assertThat(res).contains("applicationId 'SOME_ID'");
		assertThat(res).contains("targetSdkVersion 2");
		assertThat(res).contains("versionCode 3");
		assertThat(res).contains("versionName \"1.2.3\"");
	}
}
