package jadx.tests.integration.debuginfo;

import java.lang.ref.WeakReference;

import jadx.api.CommentsLevel;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLineNumbers2 extends IntegrationTest {

	public static class TestCls {
		private WeakReference<TestCls> f;

		// keep constructor at line 20
		// ---
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
		getArgs().setCommentsLevel(CommentsLevel.NONE);
		printLineNumbers();

		ClassNode cls = getClassNode(TestCls.class);
		String linesMapStr = cls.getCode().getCodeMetadata().getLineMapping().toString();
		if (isJavaInput()) {
			assertThat(linesMapStr).isEqualTo("{5=18, 8=19, 11=23, 12=24, 13=25, 14=26, 15=27, 17=29, 20=32, 21=33}");
		} else {
			assertThat(linesMapStr).isEqualTo("{5=18, 8=19, 11=23, 12=24, 13=25, 14=26, 15=27, 16=29, 18=29, 21=32, 22=33}");
		}
	}
}
