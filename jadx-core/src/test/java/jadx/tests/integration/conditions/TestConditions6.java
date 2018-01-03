package jadx.tests.integration.conditions;

import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions6 extends IntegrationTest {

	public static class TestCls {
		public boolean test(List<String> l1, List<String> l2) {
			if (l2.size() > 0) {
				l1.clear();
			}
			return l1.size() == 0;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return l1.size() == 0;"));
		assertThat(code, not(containsString("else")));
	}
}
