package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesInConstructor extends IntegrationTest {

	public static class TestCls {
		public static class Base {
			final String baseVal;

			public Base(String s) {
				this.baseVal = s;
			}
		}

		public static class Sub extends Base {
			final String result;
			String tag;

			public Sub(String s, boolean flag) {
				super(s);
				String val;
				if (flag) {
					val = "a:" + s;
				} else {
					val = "b:" + s;
				}
				this.result = val;
			}

			public Sub(String s, int mode) {
				this(s, mode > 0);
				String tag;
				if (mode == 1) {
					tag = "one";
				} else {
					tag = "other";
				}
				this.tag = tag;
			}
		}

		public void check() {
			Sub s1 = new Sub("x", true);
			assertThat(s1.baseVal).isEqualTo("x");
			assertThat(s1.result).isEqualTo("a:x");

			Sub s2 = new Sub("y", false);
			assertThat(s2.baseVal).isEqualTo("y");
			assertThat(s2.result).isEqualTo("b:y");

			Sub s3 = new Sub("z", 1);
			assertThat(s3.baseVal).isEqualTo("z");
			assertThat(s3.result).isEqualTo("a:z");
			assertThat(s3.tag).isEqualTo("one");

			Sub s4 = new Sub("w", 0);
			assertThat(s4.baseVal).isEqualTo("w");
			assertThat(s4.result).isEqualTo("b:w");
			assertThat(s4.tag).isEqualTo("other");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("super(s);")
				.containsOne("this(s, mode > 0);");
	}

	@Test
	public void testJava() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("super(s);")
				.containsOne("this(s, mode > 0);");
	}
}
