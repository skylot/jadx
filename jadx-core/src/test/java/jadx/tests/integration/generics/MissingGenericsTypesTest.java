package jadx.tests.integration.generics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class MissingGenericsTypesTest extends SmaliTest {

	static class TestCls {

		@SuppressWarnings("unused")
		private int x;

		public void test() {
			Map<String, String> map = new HashMap<>();
			x = 1;
			for (String s : map.keySet()) {
				System.out.println(s);
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("Map<String, Object>"));
	}

}
