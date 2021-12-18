package jadx.tests.integration.trycatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {"));
		assertThat(code, containsOne("e.printStackTrace();"));

		// TODO: store vararg attribute for methods from classpath
		// assertThat(code, containsOne("constructor.newInstance();"));
	}
}
