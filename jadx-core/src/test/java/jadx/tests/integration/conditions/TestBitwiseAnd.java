package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({ "PointlessBooleanExpression", "unused" })
public class TestBitwiseAnd extends IntegrationTest {

	public static class TestCls {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a & b) != false) {
				test();
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (this.a && this.b) {"));
	}

	public static class TestCls2 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a & b) != true) {
				test();
			}
		}
	}

	@Test
	public void test2() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (!this.a || !this.b) {"));
	}

	public static class TestCls3 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a & b) == false) {
				test();
			}
		}
	}

	@Test
	public void test3() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls3.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (!this.a || !this.b) {"));
	}

	public static class TestCls4 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a & b) == true) {
				test();
			}
		}
	}

	@Test
	public void test4() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls4.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (this.a && this.b) {"));
	}
}
