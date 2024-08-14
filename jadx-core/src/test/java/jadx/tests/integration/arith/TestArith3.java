package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArith3 extends IntegrationTest {

	public static class TestCls {
		public int vp;

		public void test(byte[] buffer) {
			int n = ((buffer[3] & 255) + 4) + ((buffer[2] & 15) << 8);
			while (n + 4 < buffer.length) {
				int p = (buffer[n + 2] & 255) + ((buffer[n + 1] & 31) << 8);
				int len = (buffer[n + 4] & 255) + ((buffer[n + 3] & 15) << 8);
				int c = buffer[n] & 255;
				switch (c) {
					case 27:
						this.vp = p;
						break;
				}
				n += len + 5;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (n + 4 < buffer.length) {")
				.containsOne(indent() + "n += len + 5;")
				.doesNotContain("; n += len + 5) {")
				.doesNotContain("default:");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (");
	}
}
