package jadx.tests.integration.rename;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUserRenamesMemberOrder extends IntegrationTest {

	public static class TestCls {
		public static class A {
		}

		public static class B {
		}

		public TestCls(A a) {
		}

		public TestCls(B b) {
		}

		public TestCls(A[] a) {
		}

		public TestCls(B[] b) {
		}

		public void first() {
		}

		public void second() {
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test(boolean debugInfo) {
		getArgs().setDebugInfo(debugInfo);
		addClsRename(TestCls.A.class.getName(), "Zebra");
		addClsRename(TestCls.B.class.getName(), "Alpha");
		addMthRename(TestCls.class.getName(), "first()V", "zeta");
		addMthRename(TestCls.class.getName(), "second()V", "alpha");

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().getCodeStr();
		if (debugInfo) {
			assertThat(code).containsSubsequence("(Zebra ", "(Alpha ", "(Zebra[] ", "(Alpha[] ");
			assertThat(code).containsSubsequence("void zeta()", "void alpha()");
		} else {
			assertThat(code).containsSubsequence("(Alpha ", "(Zebra ", "(Alpha[] ", "(Zebra[] ");
			assertThat(code).containsSubsequence("void alpha()", "void zeta()");
		}
		assertThat(cls).reloadCode(this).isEqualTo(code);
	}
}
