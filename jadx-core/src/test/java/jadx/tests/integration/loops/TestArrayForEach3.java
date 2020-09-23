package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Issue #977
 */
public class TestArrayForEach3 extends IntegrationTest {

	public static class TestCls {
		public void test(String[] arr) {
			for (String s : arr) {
				if (s.length() > 0) {
					return;
				}
			}
			throw new IllegalArgumentException("All strings are empty");
		}

		public void check() {
			test(new String[] { "", "a" }); // no exception
			try {
				test(new String[] { "", "" });
				fail("IllegalArgumentException expected");
			} catch (IllegalArgumentException e) {
				// expected
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("while")
				.containsOne("for (String str : strArr) {");
	}
}
