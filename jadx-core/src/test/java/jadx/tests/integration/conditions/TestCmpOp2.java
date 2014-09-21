package jadx.tests.integration.conditions;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

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
		System.out.println(code);

		assertThat(code, containsString("return a > b;"));
		assertThat(code, containsString("return ((double) c) < d;"));
	}
}
