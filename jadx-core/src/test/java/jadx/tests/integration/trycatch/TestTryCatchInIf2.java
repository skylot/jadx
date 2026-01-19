package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

// #2384
public class TestTryCatchInIf2 extends IntegrationTest {
	public static class TestCls {
		public void test(Class<?> cls) {
			Object obj = null;
			if (cls != null) {
				try {
					obj = cls.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					System.out.println("error");
				}
			}
			System.out.println("obj = " + obj);
		}
	}

	@Test
	public void test() {
		// happens only without debug info and java version >= 10
		noDebugInfo();
		useTargetJavaVersion(10);
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code();
	}
}
