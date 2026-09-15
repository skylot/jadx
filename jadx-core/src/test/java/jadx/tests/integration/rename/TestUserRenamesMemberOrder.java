package jadx.tests.integration.rename;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUserRenamesMemberOrder extends IntegrationTest {

	public static class TestCls {
		public static int z = Integer.parseInt("1");
		public static int a = Integer.parseInt("2");

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
	@CsvSource({ "false, false", "false, true", "true, false", "true, true" })
	public void test(boolean debugInfo, boolean stableMemberOrder) {
		getArgs().setDebugInfo(debugInfo);
		getArgs().setStableMemberOrder(stableMemberOrder);
		addClsRename(TestCls.A.class.getName(), "Zebra");
		addClsRename(TestCls.B.class.getName(), "Alpha");
		addMthRename(TestCls.class.getName(), "first()V", "zeta");
		addMthRename(TestCls.class.getName(), "second()V", "alpha");

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().getCodeStr();
		assertThat(code).containsSubsequence("static int z =", "static int a =");
		if (debugInfo || !stableMemberOrder) {
			assertThat(code).containsSubsequence("(Zebra ", "(Alpha ", "(Zebra[] ", "(Alpha[] ");
			assertThat(code).containsSubsequence("void zeta()", "void alpha()");
		} else {
			assertThat(code).containsSubsequence("(Alpha ", "(Zebra ", "(Alpha[] ", "(Zebra[] ");
			assertThat(code).containsSubsequence("void alpha()", "void zeta()");
		}
		assertThat(cls).reloadCode(this).isEqualTo(code);
	}
}
