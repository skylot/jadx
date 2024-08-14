package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestStringBuilderElimination4Neg extends IntegrationTest {

	public static class TestCls<K, V> {
		private K k;
		private V v;

		public String test() {
			StringBuilder sb = new StringBuilder();
			sb.append(k);
			sb.append('=');
			sb.append(v);
			return sb.toString();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("sb.append('=');");
	}
}
