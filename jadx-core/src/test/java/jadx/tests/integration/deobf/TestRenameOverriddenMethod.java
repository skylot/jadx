package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRenameOverriddenMethod extends IntegrationTest {

	public static class TestCls {
		public interface I {
			void m();
		}

		public static class A implements I {
			@Override
			public void m() {
			}
		}

		public static class B extends A {
			@Override
			public void m() {
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
				.countString(2, "@Override")
				.countString(3, "/* renamed from: m */")
				.containsOne("void mo0m();")
				.countString(2, "public void mo0m() {");
	}
}
