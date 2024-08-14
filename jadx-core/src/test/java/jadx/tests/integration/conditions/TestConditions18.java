package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("CommentedOutCode")
public class TestConditions18 extends SmaliTest {

	// @formatter:off
	/*
		public static class TestConditions18 {
			private Map map;

			public boolean test(Object obj) {
				return this == obj || ((obj instanceof TestConditions18) && st(this.map, ((TestConditions18) obj).map));
			}

			private static boolean st(Object obj, Object obj2) {
				return false;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsLines(2,
						"if (this != obj) {",
						indent() + "return (obj instanceof TestConditions18) && st(this.map, ((TestConditions18) obj).map);",
						"}",
						"return true;");
	}

	@Test
	@NotYetImplemented
	public void testNYI() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("return this == obj || ((obj instanceof TestConditions18) && st(this.map, ((TestConditions18) obj).map));");
	}
}
