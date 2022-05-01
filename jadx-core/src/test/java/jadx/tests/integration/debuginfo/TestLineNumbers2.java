package jadx.tests.integration.debuginfo;

import java.lang.ref.WeakReference;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

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

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		printLineNumbers();

		ClassNode cls = getClassNode(TestCls.class);
		String linesMapStr = cls.getCode().getCodeMetadata().getLineMapping().toString();
		if (isJavaInput()) {
			assertEquals("{6=16, 9=17, 12=21, 13=22, 14=23, 15=24, 16=25, 18=27, 21=30, 22=31}", linesMapStr);
		} else {
			assertEquals("{6=16, 9=17, 12=21, 13=22, 14=23, 15=24, 16=25, 17=27, 19=27, 22=30, 23=31}", linesMapStr);
		}
	}
}
