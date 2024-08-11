package jadx.tests.integration.loops;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestNestedLoops extends IntegrationTest {

	public static class TestCls {

		public void test(List<String> l1, List<String> l2) {
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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (String s1 : l1) {")
				.containsOne("for (String s2 : l2) {")
				.containsOne("if (s1.equals(s2)) {")
				.containsOne("l2.add(s1);")
				.containsOne("l1.remove(s2);");
	}
}
