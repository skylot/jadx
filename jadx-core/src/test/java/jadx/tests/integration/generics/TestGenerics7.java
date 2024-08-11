package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics7 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			declare(String.class);
		}

		public <T> T declare(Class<T> cls) {
			return null;
		}

		public void declare(Object cls) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("declare(String.class);");
	}
}
