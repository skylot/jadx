package jadx.tests.integration.variables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestVariables4 extends IntegrationTest {

	public static class TestCls {
		private static boolean runTest(String clsName) {
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
						+ (pass ? "PASS" : "FAIL") + "\t"
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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("} catch (InvocationTargetException e) {"));
		assertThat(code, containsString("pass = false;"));
		assertThat(code, containsString("exc = e.getCause();"));
		assertThat(code, containsString("System.err.println(\"Class '\" + clsName + \"' not found\");"));
		assertThat(code, containsString("return pass;"));
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
