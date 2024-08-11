package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmaliWithPath("generics", "MissingGenericsTypesTest"))
				.code()
				.contains("Map<String");
	}
}
