package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TestOuterConstructorCall extends IntegrationTest {

	public static class TestCls {
		private TestCls(Inner inner) {
			System.out.println(inner);
		}

		private class Inner {
			private TestCls test() {
				return new TestCls(this);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("private class Inner {"));
		assertThat(code, containsString("return new TestOuterConstructorCall$TestCls(this);"));
		assertThat(code, not(containsString("synthetic")));
	}
}
