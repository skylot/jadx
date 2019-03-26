package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPrimitivesInIf extends IntegrationTest {

	public static class TestCls {

		public boolean test(String str) {
			short sh = Short.parseShort(str);
			int i = Integer.parseInt(str);
			System.out.println(sh + " vs " + i);
			return sh == i;
		}

		public void check() {
			assertTrue(test("1"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("short sh = Short.parseShort(str);"));
		assertThat(code, containsOne("int i = Integer.parseInt(str);"));
		assertThat(code, containsOne("return sh == i;"));
	}

	@Test
	public void test2() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("short parseShort = Short.parseShort(str);"));
	}
}
