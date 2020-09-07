package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #962
 */
public class TestFieldCast extends IntegrationTest {

	public static class TestCls {
		public static class A {
			public boolean publicField;
			boolean packagePrivateField;
			protected boolean protectedField;
			private boolean privateField;
		}

		public static class B extends A {
			public void test() {
				((A) this).publicField = false;
				((A) this).protectedField = false;
				((A) this).packagePrivateField = false;
				((A) this).privateField = false; // cast to 'A' needed only here
			}
		}

		public static class C {
			public void test(B b) {
				((A) b).publicField = false;
				((A) b).protectedField = false;
				((A) b).packagePrivateField = false;
				((A) b).privateField = false; // cast to 'A' needed only here
			}
		}

		private static class D {
			public <T extends B> void test(T t) {
				((A) t).publicField = false;
				((A) t).protectedField = false;
				((A) t).packagePrivateField = false;
				((A) t).privateField = false; // cast to 'A' needed only here
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("((A) this)")
				.containsOne("((A) b)")
				.containsOne("((A) t)")
				.doesNotContain("unused =")
				.doesNotContain("access modifiers changed");
	}
}
