package jadx.tests.integration.loops;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIndexedLoop extends IntegrationTest {

	public static class TestCls {

		public File test(File[] files) {
			File file = null;
			if (files != null) {
				int length = files.length;
				if (length == 0) {
					file = null;
				} else {
					for (int i = 0; i < length; i++) {
						file = files[i];
						if (file.getName().equals("f")) {
							break;
						}
					}
				}
			} else {
				file = null;
			}
			if (file != null) {
				file.deleteOnExit();
			}
			return file;
		}

		public void check() {
			assertThat(test(null)).isNull();
			assertThat(test(new File[] {})).isNull();

			File file = new File("f");
			assertThat(test(new File[] { new File("a"), file })).isEqualTo(file);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (File file :")
				.containsOne("for (int i = 0; i < length; i++) {");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (File file :")
				.containsOne("for (int i = 0; i < length; i++) {");
	}
}
