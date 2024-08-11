package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFinallyExtract extends IntegrationTest {

	public static class TestCls {
		private int result = 0;

		public String test() {
			boolean success = false;
			try {
				String value = call();
				result++;
				success = true;
				return value;
			} finally {
				if (!success) {
					result -= 2;
				}
			}
		}

		private String call() {
			return "call";
		}

		public void check() {
			test();
			assertThat(result).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} finally {")
				.doesNotContain("if (0 == 0) {")

				.containsOne("boolean success = false;")
				.containsOne("try {")
				.containsOne("success = true;")
				.containsOne("return value;")
				.containsOne("if (!success) {");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.result++;")
				.containsOne("} catch (Throwable th) {")
				.containsOne("this.result -= 2;")
				.containsOne("throw th;");

		// java compiler optimization: 'success' variable completely removed and no code duplication:
		// @formatter:off
		/*
		    public String test() {
		        try {
		            String call = call();
		            this.result++;
		            return call;
		        } catch (Throwable th) {
		            this.result -= 2;
		            throw th;
		        }
		    }
		*/
		// @formatter:on
	}
}
