package jadx.tests.integration.others;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestIfInTry extends IntegrationTest {

	public static class TestCls {
		public File dir;

		public int test() {
			try {
				int a = f();
				if (a != 0) {
					return a;
				}
			} catch (Exception e) {
				// skip
			}
			try {
				f();
				return 1;
			} catch (IOException e) {
				return -1;
			}
		}

		private int f() throws IOException {
			return 0;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (a != 0) {"));
		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, countString(2, "try {"));
		assertThat(code, countString(3, "f()"));
		assertThat(code, containsOne("return 1;"));
		assertThat(code, containsOne("} catch (IOException e"));
		assertThat(code, containsOne("return -1;"));
	}
}
