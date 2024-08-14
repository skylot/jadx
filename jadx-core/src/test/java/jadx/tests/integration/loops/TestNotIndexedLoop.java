package jadx.tests.integration.loops;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNotIndexedLoop extends IntegrationTest {

	public static class TestCls {

		public File test(File[] files) {
			File file;
			if (files != null) {
				int length = files.length;
				if (length == 0) {
					file = null;
				} else {
					int i = 0;
					while (true) {
						if (i >= length) {
							file = null;
							break;
						}
						file = files[i];
						if (file.getName().equals("f")) {
							break;
						}
						i++;
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
				.doesNotContain("for (")
				.containsOne("while (true) {");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (")
				.containsOne("while (true) {");
	}
}
