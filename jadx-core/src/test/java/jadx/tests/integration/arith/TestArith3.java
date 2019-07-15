package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while (n + 4 < buffer.length) {"));
		assertThat(code, containsOne(indent() + "n += len + 5;"));
		assertThat(code, not(containsString("; n += len + 5) {")));
		assertThat(code, not(containsString("default:")));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while ("));
	}
}
