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
}
