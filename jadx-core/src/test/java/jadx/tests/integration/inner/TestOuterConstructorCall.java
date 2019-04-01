package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestOuterConstructorCall extends IntegrationTest {

	public static class TestCls {
		private TestCls(Inner inner) {
			System.out.println(inner);
		}

		private class Inner {
			@SuppressWarnings("unused")
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
