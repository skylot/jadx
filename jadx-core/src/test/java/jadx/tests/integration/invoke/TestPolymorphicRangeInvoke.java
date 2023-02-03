package jadx.tests.integration.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TestPolymorphicRangeInvoke extends IntegrationTest {

	public static class TestCls {
		public String func2(int a, int b, int c, int d, int e, int f) {
			return String.valueOf(a + b + c + d + e + f);
		}

		public String test() {
			try {
				MethodHandles.Lookup lookup = MethodHandles.lookup();
				MethodType methodType = MethodType.methodType(String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
						Integer.TYPE, Integer.TYPE);
				MethodHandle methodHandle = lookup.findVirtual(TestCls.class, "func2", methodType);
				return (String) methodHandle.invoke(this, 10, 20, 30, 40, 50, 60);
			} catch (Throwable e) {
				fail(e);
				return null;
			}
		}

		public void check() {
			assertThat(test()).isEqualTo("210");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8 })
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls).code()
				.containsOne("return (String) methodHandle.invoke(this, 10, 20, 30, 40, 50, 60);");
		assertThat(cls).disasmCode()
				.containsOne("invoke-polymorphic/range");
	}
}
