package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRenameOverriddenMethod3 extends IntegrationTest {

	public static class TestCls {

		public abstract static class A {
			public abstract int call();
		}

		public static class B extends A {
			@Override
			public final int call() {
				return 1;
			}
		}
	}

	@Test
	public void test() {
		addMthRename(TestCls.class.getName() + "$A", "call()I", "callRenamed");
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(1, "@Override")
				.countString(2, "int callRenamed()");
	}
}
