package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstInline extends SmaliTest {
	// @formatter:off
	/*
		private static String test(boolean b) {
			List<String> list;
			String str;
			if (b) {
				list = Collections.emptyList();
				str = "1";
			} else {
				list = null;
				str = list; // not correct assign in java but bytecode allow it
			}
			return use(list, str);
		}

		private static String use(List<String> list, String str) {
			return list + str;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithPkg("types", "TestConstInline"))
				.code()
				.containsOne("list = null;")
				.containsOne("str = null;");
	}
}
