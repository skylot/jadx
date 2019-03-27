package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestTryCatchInIf extends IntegrationTest {

	public static class TestCls {

		private String test(String name, String value) {
			if (value != null) {
				try {
					int key;
					if (value.startsWith("0x")) {
						value = value.substring(2);
						key = Integer.parseInt(value, 16);
					} else {
						key = Integer.parseInt(value);
					}
					return name + '=' + key;
				} catch (NumberFormatException e) {
					return "Failed to parse number";
				}
			}
			System.out.println("?");
			return null;
		}

		public void check() {
			assertNull(test("n", null));
			assertEquals("n=7", test("n", "7"));
			assertEquals("n=77", test("n", "0x" + Integer.toHexString(77)));
			assertEquals("Failed to parse number", test("n", "abc"));
			assertEquals("Failed to parse number", test("n", "0xabX"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("} catch (NumberFormatException e) {"));
	}
}
