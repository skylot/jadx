package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestCmpOp extends IntegrationTest {

	public static class TestCls {
		public boolean testGT(float a) {
			return a > 3.0f;
		}

		public boolean testLT(float b) {
			return b < 2.0f;
		}

		public boolean testEQ(float c) {
			return c == 1.0f;
		}

		public boolean testNE(float d) {
			return d != 0.0f;
		}

		public boolean testGE(float e) {
			return e >= -1.0f;
		}

		public boolean testLE(float f) {
			return f <= -2.0f;
		}

		public boolean testGT2(float g) {
			return 4.0f > g;
		}

		public boolean testLT2(long h) {
			return 5 < h;
		}

		public boolean testGE2(double i) {
			return 6.5d < i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return a > 3.0f;"));
		assertThat(code, containsString("return b < 2.0f;"));
		assertThat(code, containsString("return c == 1.0f;"));
		assertThat(code, containsString("return d != 0.0f;"));
		assertThat(code, containsString("return e >= -1.0f;"));
		assertThat(code, containsString("return f <= -2.0f;"));
		assertThat(code, containsString("return 4.0f > g;"));
		assertThat(code, containsString("return 5 < h;"));
		assertThat(code, containsString("return 6.5d < i;"));
	}
}
