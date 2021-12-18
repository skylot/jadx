package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("checkstyle:printstacktrace")
public class TestTryCatch7 extends IntegrationTest {

	public static class TestCls {
		public Exception test() {
			Exception e = new Exception();
			try {
				Thread.sleep(50);
			} catch (Exception ex) {
				e = ex;
			}
			e.printStackTrace();
			return e;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		check(code, "e", "ex");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		check(code, "e", "e2");
	}

	private void check(String code, String excVarName, String catchExcVarName) {
		assertThat(code, containsOne("Exception " + excVarName + " = new Exception();"));
		assertThat(code, containsOne("} catch (Exception " + catchExcVarName + ") {"));
		assertThat(code, containsOne(excVarName + " = " + catchExcVarName + ';'));
		assertThat(code, containsOne(excVarName + ".printStackTrace();"));
		assertThat(code, containsOne("return " + excVarName + ';'));
	}
}
