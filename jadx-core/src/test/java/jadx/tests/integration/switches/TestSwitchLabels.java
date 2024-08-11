package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchLabels extends IntegrationTest {
	public static class TestCls {
		public static final int CONST_ABC = 0xABC;
		public static final int CONST_CDE = 0xCDE;

		public static class Inner {
			private static final int CONST_CDE_PRIVATE = 0xCDE;

			public int f1(int arg0) {
				switch (arg0) {
					case CONST_CDE_PRIVATE:
						return CONST_ABC;
				}
				return 0;
			}
		}

		public static int f1(int arg0) {
			switch (arg0) {
				case CONST_ABC:
					return CONST_CDE;
			}
			return 0;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("case CONST_ABC")
				.contains("return CONST_CDE;")
				.doesNotContain("case CONST_CDE_PRIVATE")
				.contains(".CONST_ABC;");
	}

	@Test
	public void testWithDisabledConstReplace() {
		getArgs().setReplaceConsts(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("case CONST_ABC")
				.contains("case 2748")
				.doesNotContain("return CONST_CDE;")
				.contains("return 3294;")
				.doesNotContain("case CONST_CDE_PRIVATE")
				.doesNotContain(".CONST_ABC;");
	}
}
