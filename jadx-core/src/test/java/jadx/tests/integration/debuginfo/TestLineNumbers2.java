package jadx.tests.integration.debuginfo;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLineNumbers2 extends IntegrationTest {

	public static class TestCls {
		private WeakReference<TestCls> f;

		// keep constructor at line 18
		public TestCls(TestCls s) {
		}

		public TestCls test(TestCls s) {
			TestCls store = f != null ? f.get() : null;
			if (store == null) {
				store = new TestCls(s);
				f = new WeakReference<>(store);
			}
			return store;
		}

		public Object test2() {
			return new Object();
		}
	}

	@Test
	public void test() {
		printLineNumbers();

		ClassNode cls = getClassNode(TestCls.class);
		String linesMapStr = cls.getCode().getLineMapping().toString();
		if (isJavaInput()) {
			assertEquals("{6=16, 9=17, 12=21, 13=22, 14=23, 16=25, 18=27, 21=30}", linesMapStr);
		} else {
			// TODO: invert condition to match source lines
			assertEquals("{6=16, 9=17, 12=21, 13=22, 14=23, 15=27, 17=24, 18=25, 19=27, 22=30, 23=31}", linesMapStr);
		}
	}
}
