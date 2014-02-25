package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestNestedLoops extends InternalJadxTest {

	public static class TestCls {

		private void test(List<String> l1, List<String> l2) {
			Iterator<String> it1 = l1.iterator();
			while (it1.hasNext()) {
				String s1 = it1.next();
				Iterator<String> it2 = l2.iterator();
				while (it2.hasNext()) {
					String s2 = it2.next();
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

		assertThat(code, containsString("while (it1.hasNext()) {"));
		assertThat(code, containsString("while (it2.hasNext()) {"));
		assertThat(code, containsString("if (s1.equals(s2)) {"));
		assertThat(code, containsString("l2.add(s1);"));
	}
}
