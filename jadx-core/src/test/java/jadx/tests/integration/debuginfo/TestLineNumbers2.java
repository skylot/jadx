package jadx.tests.integration.debuginfo;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLineNumbers2 extends IntegrationTest {

	public static class TestCls {
		private WeakReference<TestCls> f;

		// keep at line 18
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
		Map<Integer, Integer> lineMapping = cls.getCode().getLineMapping();
		assertEquals("{8=18, 11=22, 12=23, 13=24, 14=28, 16=25, 17=26, 18=28, 21=31, 22=32}",
				lineMapping.toString());
	}
}
