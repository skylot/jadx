package jadx.tests.integration.others;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (a != 0) {")
				.containsOne("} catch (Exception e) {")
				.countString(2, "try {")
				.countString(3, "f()")
				.containsOne("return 1;")
				.containsOne("} catch (IOException e")
				.containsOne("return -1;");
	}
}
