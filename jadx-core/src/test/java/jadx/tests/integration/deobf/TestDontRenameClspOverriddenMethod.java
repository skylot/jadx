package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDontRenameClspOverriddenMethod extends IntegrationTest {

	public static class TestCls {

		public static class A implements Runnable {
			@Override
			public void run() {
			}
		}

		public static class B extends A {
			@Override
			public void run() {
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything

		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "public void run() {");
	}
}
