package jadx.tests.internal.debuginfo;

import jadx.api.InternalJadxTest;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestLineNumbers2 extends InternalJadxTest {

	public static class TestCls {
		private WeakReference<TestCls> f;

		public TestCls(TestCls s) {
		}

		TestCls test(TestCls s) {
			TestCls store = f != null ? f.get() : null;
			if (store == null) {
				store = new TestCls(s);
				f = new WeakReference<TestCls>(store);
			}
			return store;
		}

		public Object test2() {
			return new Object();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		CodeWriter codeWriter = cls.getCode();
		String code = codeWriter.toString();
		System.out.println(code);

		Map<Integer, Integer> lineMapping = codeWriter.getLineMapping();
		assertEquals("{8=18, 11=22, 13=23, 14=24, 15=28, 17=25, 18=26, 19=28, 22=31, 23=32}",
				lineMapping.toString());
	}
}
