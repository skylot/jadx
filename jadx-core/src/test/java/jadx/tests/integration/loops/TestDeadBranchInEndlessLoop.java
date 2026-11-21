package jadx.tests.integration.loops;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeadBranchInEndlessLoop extends IntegrationTest {

	public static class TestCls {
		private int len;

		public int test(InputStream in) throws IOException {
			while (true) {
				len = in.read();
				if (len == -1) {
					break;
				}
				if (len == 0) {
					continue;
				}
			}
			return len;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (true) {")
				.containsOne("return");
	}
}
