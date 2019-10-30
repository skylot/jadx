package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"if (this != obj) {",
				indent() + "return (obj instanceof TestConditions18) && st(this.map, ((TestConditions18) obj).map);",
				"}",
				"return true;"));
	}

	@Test
	@NotYetImplemented
	public void testNYI() {
		ClassNode cls = getClassNodeFromSmali();
		String code = cls.getCode().toString();

		assertThat(code,
				containsOne("return this == obj || ((obj instanceof TestConditions18) && st(this.map, ((TestConditions18) obj).map));"));
	}
}
