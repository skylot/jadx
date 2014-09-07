package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.util.List;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestNestedLoops extends InternalJadxTest {

	public static class TestCls {

		private void test(List<String> l1, List<String> l2) {
			for (String s1 : l1) {
				for (String s2 : l2) {
					if (s1.equals(s2)) {
						if (s1.length() == 5) {
							l2.add(s1);
						} else {
							l1.remove(s2);
						}
					}
				}
			}
			if (l2.size() > 0) {
				l1.clear();
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("for (String s1 : l1) {"));
		assertThat(code, containsOne("for (String s2 : l2) {"));
		assertThat(code, containsOne("if (s1.equals(s2)) {"));
		assertThat(code, containsOne("l2.add(s1);"));
		assertThat(code, containsOne("l1.remove(s2);"));
	}
}
