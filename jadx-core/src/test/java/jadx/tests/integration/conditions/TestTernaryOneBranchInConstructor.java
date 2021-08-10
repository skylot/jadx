package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTernaryOneBranchInConstructor extends IntegrationTest {

	public static class TestCls {
		public TestCls(String str, int i) {
			this(str == null ? 0 : i);
		}

		public TestCls(int i) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("this(str == null ? 0 : i);"));
		assertThat(code, not(containsString("//")));
		assertThat(code, not(containsString("call moved to the top of the method")));
	}

	public static class TestCls2 {
		public TestCls2(String str, int i) {
			this(i == 1 ? str : "", i == 0 ? "" : str);
		}

		public TestCls2(String a, String b) {
		}
	}

	@Test
	public void test2() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("this(i == 1 ? str : \"\", i == 0 ? \"\" : str);"));
		assertThat(code, not(containsString("//")));
	}
}
