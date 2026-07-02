package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestClassGen extends IntegrationTest {

	public static class TestCls {
		public interface I {
			int test();

			public int test3();
		}

		public abstract static class A {
			public abstract int test2();
		}
	}

	public static class GenericCls<T extends Number & Comparable<T>> {
		private final T value;

		public GenericCls(T value) {
			this.value = value;
		}

		public <R extends Runnable> R get(R runnable) {
			return runnable;
		}
	}

	public static class MultiInterfaceCls implements Runnable, AutoCloseable {
		@Override
		public void run() {
		}

		@Override
		public void close() {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public interface I {")
				.contains(indent(2) + "int test();")
				.doesNotContain("public int test();")
				.contains(indent(2) + "int test3();")
				.contains("public static abstract class A {")
				.contains(indent(2) + "public abstract int test2();");
	}

	@Test
	public void testGenericBounds() {
		JadxAssertions.assertThat(getClassNode(GenericCls.class))
				.code()
				.contains("public class TestClassGen$GenericCls<T extends Number & Comparable<T>> {")
				.contains("private final T value;")
				.contains("public TestClassGen$GenericCls(T value) {")
				.contains("public <R extends Runnable> R get(R runnable) {");
	}

	@Test
	public void testMultipleInterfaces() {
		JadxAssertions.assertThat(getClassNode(MultiInterfaceCls.class))
				.code()
				.contains("public class TestClassGen$MultiInterfaceCls implements Runnable, AutoCloseable {")
				.contains("public void run() {")
				.contains("public void close() {");
	}
}
