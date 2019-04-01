package jadx.tests.integration.loops;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestIterableForEach3 extends IntegrationTest {

	public static class TestCls<T extends String> {
		private Set<T> a;
		private Set<T> b;

		public void test(T str) {
			Set<T> set = str.length() == 1 ? a : b;
			for (T s : set) {
				if (s.length() == str.length()) {
					if (str.length() == 0) {
						set.remove(s);
					} else {
						set.add(str);
					}
					return;
				}
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (T s : set) {"));
		assertThat(code, containsOne("if (str.length() == 0) {"));
		// TODO move return outside 'if'
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
