package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums extends IntegrationTest {

	public static class TestCls {

		public enum EmptyEnum {
		}

		@SuppressWarnings("NoWhitespaceBefore")
		public enum EmptyEnum2 {
			;

			public static void mth() {
			}
		}

		public enum Direction {
			NORTH,
			SOUTH,
			EAST,
			WEST
		}

		public enum Singleton {
			INSTANCE;

			public String test() {
				return "";
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(1, "public enum EmptyEnum {", "}")
				.containsLines(1,
						"public enum EmptyEnum2 {",
						indent(1) + ';',
						"",
						indent(1) + "public static void mth() {",
						indent(1) + '}',
						"}")
				.containsLines(1, "public enum Direction {",
						indent(1) + "NORTH,",
						indent(1) + "SOUTH,",
						indent(1) + "EAST,",
						indent(1) + "WEST",
						"}")
				.containsLines(1, "public enum Singleton {",
						indent(1) + "INSTANCE;",
						"",
						indent(1) + "public String test() {",
						indent(2) + "return \"\";",
						indent(1) + '}',
						"}");
	}
}
