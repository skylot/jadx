package jadx.tests.integration.loops;

import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestNestedLoops2 extends IntegrationTest {

	public static class TestCls {

		private boolean test(List<String> list) {
			int j = 0;
			for (int i = 0; i < list.size(); i++) {
				String s = list.get(i);
				while (j < s.length()) {
					j++;
				}
			}
			return j > 10;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (int i = 0; i < list.size(); i++) {"));
		assertThat(code, containsOne("while (j < ((String) list.get(i)).length()) {"));
	}
}
