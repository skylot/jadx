package jadx.tests.integration.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TestPolymorphicInvoke extends SmaliTest {

	public static class TestCls {
		public String func(int a, int c) {
			return String.valueOf(a + c);
		}

		public String test() {
			try {
				MethodType methodType = MethodType.methodType(String.class, Integer.TYPE, Integer.TYPE);
				MethodHandle methodHandle = MethodHandles.lookup().findVirtual(TestCls.class, "func", methodType);
				return (String) methodHandle.invoke(this, 1, 2);
			} catch (Throwable e) {
				fail(e);
				return null;
			}
		}

		public void check() {
			assertThat(test()).isEqualTo("3");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11 })
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOne("return (String) methodHandle.invoke(this, 1, 2);");
		assertThat(cls).disasmCode()
				.containsOne("invoke-polymorphic");
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.JAVA11 })
	public void testJava() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return (String) methodHandle.invoke(this, 1, 2);");
		// java uses 'invokevirtual'
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("String ret = (String) methodHandle.invoke(this, 10, 20);");
	}
}
