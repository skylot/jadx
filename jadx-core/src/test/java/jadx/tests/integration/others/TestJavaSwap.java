package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestJavaSwap extends RaungTest {

	@SuppressWarnings("StringBufferReplaceableByString")
	public static class TestCls {
		private Iterable<String> field;

		@Override
		public String toString() {
			String string = String.valueOf(this.field);
			return new StringBuilder(8 + String.valueOf(string).length())
					.append("concat(").append(string).append(")")
					.toString();
		}
	}

	@Test
	public void testJava() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void test() {
		useJavaInput();
		assertThat(getClassNodeFromRaung())
				.code();
	}
}
