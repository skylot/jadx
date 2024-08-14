package jadx.tests.integration.loops;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopDetection5 extends IntegrationTest {

	public static class TestCls {

		public String test(String str) {
			Iterator<String> it = getStrings().iterator();
			String otherStr = null;
			while (it.hasNext()) {
				otherStr = it.next();
				if (otherStr.equalsIgnoreCase(str)) {
					break;
				}
			}
			return otherStr;
		}

		private List<String> getStrings() {
			return Arrays.asList("str", "otherStr", "STR", "OTHERSTR");
		}

		public void check() {
			assertThat(test("OTHERSTR")).isEqualTo("otherStr");
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (")
				.containsOne("it.next();");
	}
}
