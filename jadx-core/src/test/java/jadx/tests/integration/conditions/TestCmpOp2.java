package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCmpOp2 extends IntegrationTest {

	public static class TestCls {
		public boolean testGT(float a, float b) {
			return a > b;
		}

		public boolean testLT(float c, double d) {
			return c < d;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return a > b;"));
		assertThat(code, containsString("return ((double) c) < d;"));
	}
}
