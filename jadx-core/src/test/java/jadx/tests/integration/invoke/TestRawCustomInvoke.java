package jadx.tests.integration.invoke;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TestRawCustomInvoke extends SmaliTest {

	public static class TestCls {

		public static String func(int a, double b) {
			return String.valueOf(a + b);
		}

		private static CallSite staticBootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
			try {
				return new ConstantCallSite(lookup.findStatic(lookup.lookupClass(), name, type));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		public String test() {
			try {
				return (String) staticBootstrap(MethodHandles.lookup(), "func",
						MethodType.methodType(String.class, Integer.TYPE, Double.TYPE))
								.dynamicInvoker().invoke(1, 2.0d);
			} catch (Throwable e) {
				fail(e);
				return null;
			}
		}

		public void check() {
			assertThat(test()).isEqualTo("3.0");
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		// this code does not contain `invoke-custom` instruction
		// only check if equivalent polymorphic call is correct
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(
						"return (String) staticBootstrap(MethodHandles.lookup(), \"func\", MethodType.methodType(String.class, Integer.TYPE, Double.TYPE)).dynamicInvoker().invoke(1, 2.0d);");
	}

	@Test
	public void testSmali() {
		forceDecompiledCheck();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne(
						"return (String) staticBootstrap(MethodHandles.lookup(), \"func\", MethodType.methodType(String.class, Integer.TYPE, Double.TYPE)).dynamicInvoker().invoke(1, 2.0d) /* invoke-custom */;");
	}
}
