package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInheritedMethodRename extends IntegrationTest {

	public static class TestCls {

		public static class A extends B {
		}

		public static class B {
			public void call() {
				System.out.println("call");
			}
		}

		public void test(A a) {
			// reference to A.call() not renamed,
			// should be resolved to B.call() and use alias
			a.call();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		enableDeobfuscation();
		getArgs().setDeobfuscationMinLength(99);

		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public void m1call() {")
				.doesNotContain(".call();")
				.containsOne(".m1call();");
	}
}
