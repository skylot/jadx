package jadx.tests.integration.generics;

import java.util.List;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenericFieldsRawType extends IntegrationTest {

	@SuppressWarnings("rawtypes")
	public static class TestCls {
		public static class Box<T extends Number> {
			public T value;
			public T[] arr;
			public List<T> list;

			public T getValue() {
				return this.value;
			}
		}

		public static class UnboundedBox<T> {
			public T value;
			public T[] arr;
		}

		public static boolean isNull(Box box) {
			return box.value == null;
		}

		public static boolean arrNull(Box box) {
			return box.arr == null;
		}

		public static boolean listNull(Box box) {
			return box.list == null;
		}

		public static boolean unboundedIsNull(UnboundedBox box) {
			return box.value == null;
		}

		public static boolean unboundedArrNull(UnboundedBox box) {
			return box.arr == null;
		}

		public static boolean wildcardIsNull(Box<?> box) {
			return box.value == null;
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return this.value;")
				.countString(3, "return box.value == null;")
				.countString(2, "return box.arr == null;")
				.containsOne("return box.list == null;")
				.doesNotContain("== 0");
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("== 0");
	}
}
