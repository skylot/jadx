package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class MissingGenericsTypesTest extends SmaliTest {
	// @formatter:off
	/*
		private int x;

		public void test() {
			Map<String, String> map = new HashMap();
			x = 1;
			for (String s : map.keySet()) {
				System.out.println(s);
			}
		}
	*/
	// @formatter:on

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("generics", "MissingGenericsTypesTest");
		String code = cls.getCode().toString();

		assertThat(code, containsString("Map<String"));
	}
}
