package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics3 extends IntegrationTest {

	public static class TestCls {

		public static void mthExtendsArray(List<? extends byte[]> list) {
		}

		public static void mthSuperArray(List<? super int[]> list) {
		}

		public static void mthSuperInteger(List<? super Integer> list) {
		}

		public static void mthExtendsString(List<? super String> list) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("mthExtendsArray(List<? extends byte[]> list)")
				.contains("mthSuperArray(List<? super int[]> list)")
				.contains("mthSuperInteger(List<? super Integer> list)")
				.contains("mthExtendsString(List<? super String> list)");
	}
}
