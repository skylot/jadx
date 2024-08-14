package jadx.tests.integration.trycatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

@SuppressWarnings("checkstyle:printstacktrace")
public class TestMultiExceptionCatch2 extends IntegrationTest {

	public static class TestCls {
		public void test(Constructor<?> constructor) {
			try {
				constructor.newInstance();
			} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void test() {
		commonChecks();
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		commonChecks();
	}

	private void commonChecks() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {")
				.containsOne("e.printStackTrace();");

		// TODO: store vararg attribute for methods from classpath
		// assertThat(code, containsOne("constructor.newInstance();"));
	}
}
