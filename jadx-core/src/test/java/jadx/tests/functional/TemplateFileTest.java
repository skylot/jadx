package jadx.tests.functional;

import org.junit.jupiter.api.Test;

import jadx.core.export.TemplateFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TemplateFileTest {

	@Test
	public void testBuildGradle() throws Exception {
		TemplateFile tmpl = TemplateFile.fromResources("/export/app.build.gradle.tmpl");
		tmpl.add("applicationId", "SOME_ID");
		tmpl.add("minSdkVersion", 1);
		tmpl.add("targetSdkVersion", 2);
		tmpl.add("versionCode", 3);
		tmpl.add("versionName", "1.2.3");
		String res = tmpl.build();
		System.out.println(res);

		assertThat(res, containsString("applicationId 'SOME_ID'"));
		assertThat(res, containsString("targetSdkVersion 2"));
		assertThat(res, containsString("versionCode 3"));
		assertThat(res, containsString("versionName \"1.2.3\""));
	}
}
