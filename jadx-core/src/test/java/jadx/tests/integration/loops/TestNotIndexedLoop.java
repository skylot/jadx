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

		assertThat(code, not(containsString("for (")));
		assertThat(code, containsOne("while (true) {"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (")));
		assertThat(code, containsOne("while (true) {"));
	}
}
