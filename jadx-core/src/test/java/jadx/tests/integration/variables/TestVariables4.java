package jadx.tests.integration.variables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

@SuppressWarnings("checkstyle:printstacktrace")
public class TestVariables4 extends IntegrationTest {

	public static class TestCls {
		public static boolean runTest(String clsName) {
			try {
				boolean pass = false;
				String msg = null;
				Throwable exc = null;

				Class<?> cls = Class.forName(clsName);
				if (cls.getSuperclass() == AbstractTest.class) {
					Method mth = cls.getMethod("testRun");
					try {
						AbstractTest test = (AbstractTest) cls.getConstructor().newInstance();
						pass = (Boolean) mth.invoke(test);
					} catch (InvocationTargetException e) {
						pass = false;
						exc = e.getCause();
					} catch (Throwable e) {
						pass = false;
						exc = e;
					}
				} else {
					msg = "not extends AbstractTest";
				}
				System.err.println(">> "
						+ (pass ? "PASS" : "FAIL") + '\t'
						+ clsName
						+ (msg == null ? "" : "\t - " + msg));
				if (exc != null) {
					exc.printStackTrace();
				}
				return pass;
			} catch (ClassNotFoundException e) {
				System.err.println("Class '" + clsName + "' not found");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		private static class AbstractTest {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("} catch (InvocationTargetException e) {")
				.contains("pass = false;")
				.contains("exc = e.getCause();")
				.contains("System.err.println(\"Class '\" + clsName + \"' not found\");")
				.contains("return pass;");
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
