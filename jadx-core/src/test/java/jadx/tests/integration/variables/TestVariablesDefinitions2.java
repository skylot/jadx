package jadx.tests.integration.variables;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("int i = 0;")
				.containsOne("i++;")
				.containsOne("return i;")
				.doesNotContain("i2;");
	}
}
