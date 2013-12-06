package jadx.tests.internal.inline;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestInline extends InternalJadxTest {

	public static class TestCls extends Exception {
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
