package jadx.tests.integration.loops;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (this.iterator.hasNext()) {")
				.containsOne("if (filtered != null) {")
				.containsOne("return filtered;")
				.containsOne("return null;");
	}
}
