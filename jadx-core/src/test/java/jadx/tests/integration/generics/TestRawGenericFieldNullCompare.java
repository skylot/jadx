package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRawGenericFieldNullCompare extends IntegrationTest {

	public static class TestCls {
		public static class NumBox<T extends Number> {
			public T value;
		}

		public static class MultiBox<T extends Number & Comparable<T>> {
			public T value;
		}

		public static class CompBox<T extends Comparable<T>> {
			public T value;
		}

		public static class ArrBox<T extends Number> {
			public T[] values;
		}

		public static boolean isNull(NumBox box) {
			return box.value == null;
		}

		public static int useMulti(MultiBox box) {
			if (box.value == null) {
				return -1;
			}
			return box.value.intValue();
		}

		public static boolean compNull(CompBox box) {
			return box.value == null;
		}

		public static boolean arrNull(ArrBox box) {
			return box.values == null;
		}
	}

	@Test
	public void test() {
		useJavaInput();
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("== null")
				.doesNotContain("== 0")
				.doesNotContain("Type inference failed");
	}
}
