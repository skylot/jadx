package jadx.tests.integration.variables;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestVariablesDefinitions2 extends IntegrationTest {

	public static class TestCls {

		public static int test(List<String> list) {
			int i = 0;
			if (list != null) {
				for (String str : list) {
					if (str.isEmpty()) {
						i++;
					}
				}
			}
			return i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("int i = 0;"));
		assertThat(code, containsOne("i++;"));
		assertThat(code, containsOne("return i;"));
		assertThat(code, not(containsString("i2;")));
	}
}
