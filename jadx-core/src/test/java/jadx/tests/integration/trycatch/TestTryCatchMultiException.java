package jadx.tests.integration.trycatch;

import java.security.ProviderException;
import java.time.DateTimeException;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchMultiException extends IntegrationTest {

	public static class TestCls {
		public void test() {
			try {
				System.out.println("Test");
			} catch (ProviderException | DateTimeException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		String catchExcVarName = "e";
		assertThat(code, containsOne("} catch (ProviderException | DateTimeException " + catchExcVarName + ") {"));
		assertThat(code, containsOne("throw new RuntimeException(" + catchExcVarName + ");"));
	}
}
