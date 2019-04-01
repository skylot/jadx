package jadx.tests.integration.loops;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestLoopDetection4 extends IntegrationTest {

	public static class TestCls {
		private Iterator<String> iterator;
		private SomeCls filter;

		public String test() {
			while (iterator.hasNext()) {
				String next = iterator.next();
				String filtered = filter.filter(next);
				if (filtered != null) {
					return filtered;
				}
			}
			return null;
		}

		private class SomeCls {
			public String filter(String str) {
				return str;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while (this.iterator.hasNext()) {"));
		assertThat(code, containsOne("if (filtered != null) {"));
		assertThat(code, containsOne("return filtered;"));
		assertThat(code, containsOne("return null;"));
	}
}
