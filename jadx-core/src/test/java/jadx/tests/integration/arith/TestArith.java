package jadx.tests.integration.arith;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestArith extends IntegrationTest {

	public static class TestCls {

		public void method(int a) {
			a += 2;
		}

		public void method2(int a) {
			a++;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("a += 2;"));
		assertThat(code, containsString("a++;"));
	}
}
