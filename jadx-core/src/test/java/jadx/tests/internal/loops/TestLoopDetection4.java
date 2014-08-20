package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.util.Iterator;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopDetection4 extends InternalJadxTest {

	public static class TestCls {
		private Iterator<String> iterator;
		private SomeCls filter;

		private String test() {
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
		System.out.println(code);

		assertThat(code, containsOne("while (this.iterator.hasNext()) {"));
		assertThat(code, containsOne("if (filtered != null) {"));
		assertThat(code, containsOne("return filtered;"));
		assertThat(code, containsOne("return null;"));
	}
}
