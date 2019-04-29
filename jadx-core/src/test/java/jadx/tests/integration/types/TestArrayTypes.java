package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayTypes extends IntegrationTest {

	public static class TestCls {

		public void test() {
			Exception e = new Exception();
			System.out.println(e);
			use(new Object[] { e });
		}

		public void use(Object[] arr) {
		}

		public void check() {
			test();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("use(new Object[]{e});"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("use(new Object[]{exc});"));
	}
}
