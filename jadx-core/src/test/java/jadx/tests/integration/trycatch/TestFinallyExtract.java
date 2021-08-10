package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
			assertEquals(1, result);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} finally {"));
		assertThat(code, not(containsString("if (0 == 0) {")));

		assertThat(code, containsOne("boolean success = false;"));
		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("success = true;"));
		assertThat(code, containsOne("return value;"));
		assertThat(code, containsOne("if (!success) {"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

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

		assertThat(code, containsOne("this.result++;"));
		assertThat(code, containsOne("} catch (Throwable th) {"));
		assertThat(code, containsOne("this.result -= 2;"));
		assertThat(code, containsOne("throw th;"));
	}
}
