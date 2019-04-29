package jadx.tests.integration.loops;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

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
			assertThat(test(null), nullValue());
			assertThat(test(new File[] {}), nullValue());

			File file = new File("f");
			assertThat(test(new File[] { new File("a"), file }), is(file));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (File file :")));
		assertThat(code, containsOne("for (int i = 0; i < length; i++) {"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (File file :")));
		assertThat(code, containsOne("for (int i = 0; i < length; i++) {"));
	}
}
