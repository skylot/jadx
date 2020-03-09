package jadx.tests.integration.types;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver12 extends IntegrationTest {

	public abstract static class TestCls<T> {
		private WeakReference<T> ref;

		public void test(String str) {
			T obj = this.ref.get();
			if (obj != null) {
				call(obj, str);
			}
		}

		public abstract void call(T t, String str);
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("T obj = this.ref.get();");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("Object obj")
				.containsOne("T t = this.ref.get();");
	}
}
