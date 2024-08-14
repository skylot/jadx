package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeboxing extends IntegrationTest {

	public static class TestCls {
		public Object testInt() {
			return 1;
		}

		public Object testBoolean() {
			return true;
		}

		public Object testByte() {
			return (byte) 2;
		}

		public Short testShort() {
			return 3;
		}

		public Character testChar() {
			return 'c';
		}

		public Long testLong() {
			return 4L;
		}

		public void testConstInline() {
			Boolean v = true;
			use(v);
			use(v);
		}

		private void use(Boolean v) {
		}

		public void check() {
			// don't mind weird comparisons
			// need to get primitive without using boxing or literal
			// otherwise will get same result after decompilation
			assertThat(testInt()).isEqualTo(Integer.sum(0, 1));
			assertThat(testBoolean()).isEqualTo(Boolean.TRUE);
			assertThat(testByte()).isEqualTo(Byte.parseByte("2"));
			assertThat(testShort()).isEqualTo(Short.parseShort("3"));
			assertThat(testChar()).isEqualTo("c".charAt(0));
			assertThat(testLong()).isEqualTo(Long.valueOf("4"));
			testConstInline();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return 1;")
				.containsOne("return true;")
				.containsOne("return (byte) 2;")
				.containsOne("return (short) 3;")
				.containsOne("return 'c';")
				.containsOne("return 4L;")
				.countString(2, "use(true);");
	}
}
