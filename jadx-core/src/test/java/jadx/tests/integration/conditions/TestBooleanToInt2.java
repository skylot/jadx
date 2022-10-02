package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestBooleanToInt2 extends SmaliTest {

	// @formatter:off
	/*
		public static class TestCls {
			public void test() {
				boolean v = getValue();
				use1(Integer.valueOf(v));
				use2(v);
			}

			private boolean getValue() {
				return false;
			}

			private void use1(Integer v) {
			}

			private void use2(int v) {
			}
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("use1(Integer.valueOf(value ? 1 : 0));")
				.containsOne("use2(value ? 1 : 0);");
	}
}
