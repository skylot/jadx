package jadx.tests.integration.loops;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
			assertThat(test("OTHERSTR"), is("otherStr"));
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (")));
		assertThat(code, containsOne("it.next();"));
	}
}
