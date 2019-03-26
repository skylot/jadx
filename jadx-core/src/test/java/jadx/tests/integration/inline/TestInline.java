package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInline extends IntegrationTest {

	public static class TestCls {
		public static void main(String[] args) throws Exception {
			System.out.println("Test: " + new TestCls().testRun());
		}

		private boolean testRun() {
			return false;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("System.out.println(\"Test: \" + new TestInline$TestCls().testRun());"));
	}
}
