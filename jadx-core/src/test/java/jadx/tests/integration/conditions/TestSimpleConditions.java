package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestSimpleConditions extends IntegrationTest {

	public static class TestCls {
		public boolean test1(boolean[] a) {
			return (a[0] && a[1] && a[2]) || (a[3] && a[4]);
		}

		public boolean test2(boolean[] a) {
			return a[0] || a[1] || a[2] || a[3];
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return (a[0] && a[1] && a[2]) || (a[3] && a[4]);"));
		assertThat(code, containsString("return a[0] || a[1] || a[2] || a[3];"));
	}
}
