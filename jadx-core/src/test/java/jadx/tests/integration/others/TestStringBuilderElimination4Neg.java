package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("sb.append('=');"));
	}
}
