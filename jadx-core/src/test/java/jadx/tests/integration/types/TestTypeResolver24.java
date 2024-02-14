package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver24 extends SmaliTest {

	@SuppressWarnings("DataFlowIssue")
	public static class TestCls {
		public void test() {
			((T1) null).foo1();
			((T2) null).foo2();
		}

		static class T1 {
			public void foo1() {
			}
		}

		static class T2 {
			public void foo2() {
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("((T1) null).foo1();")
				.containsOne("((T2) null).foo2();");
	}

	@Test
	public void testSmali() {
		assertThat(searchCls(loadFromSmaliFiles(), "Test1"))
				.code()
				.containsOne("((T1) null).foo1();")
				.containsOne("((T2) null).foo2();")
				.doesNotContain("T1 ")
				.doesNotContain("T2 ");
	}
}
