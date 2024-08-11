package jadx.tests.integration.trycatch;

import java.security.ProviderException;
import java.time.DateTimeException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		String catchExcVarName = "e";
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} catch (ProviderException | DateTimeException " + catchExcVarName + ") {")
				.containsOne("throw new RuntimeException(" + catchExcVarName + ");");
	}
}
