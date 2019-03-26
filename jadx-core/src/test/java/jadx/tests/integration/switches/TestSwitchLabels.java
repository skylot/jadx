package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, containsString("case CONST_ABC"));
		assertThat(code, containsString("return CONST_CDE;"));

		cls.addInnerClass(getClassNode(TestCls.Inner.class));
		assertThat(code, not(containsString("case CONST_CDE_PRIVATE")));
		assertThat(code, containsString(".CONST_ABC;"));
	}

	@Test
	public void testWithDisabledConstReplace() {
		getArgs().setReplaceConsts(false);

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, not(containsString("case CONST_ABC")));
		assertThat(code, containsString("case 2748"));
		assertThat(code, not(containsString("return CONST_CDE;")));
		assertThat(code, containsString("return 3294;"));

		cls.addInnerClass(getClassNode(TestCls.Inner.class));
		assertThat(code, not(containsString("case CONST_CDE_PRIVATE")));
		assertThat(code, not(containsString(".CONST_ABC;")));
	}
}
