package jadx.tests.integration.types;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestTypeResolver3 extends IntegrationTest {

	public static class TestCls {

		public int test(String s1, String s2) {
			int cmp = s2.compareTo(s1);
			if (cmp != 0) {
				return cmp;
			}
			return s1.length() == s2.length() ? 0 : s1.length() < s2.length() ? -1 : 1;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// TODO inline into return
		assertThat(code, containsOne("s1.length() == s2.length() ? 0 : s1.length() < s2.length() ? -1 : 1;"));
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
