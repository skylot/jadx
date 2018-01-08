package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestArrayForEachNegative extends IntegrationTest {

	public static class TestCls {

		private int test(int[] a, int[] b) {
			int sum = 0;
			for (int i = 0; i < a.length; i += 2) {
				sum += a[i];
			}
			for (int i = 1; i < a.length; i++) {
				sum += a[i];
			}
			for (int i = 0; i < a.length; i--) {
				sum += a[i];
			}
			for (int i = 0; i <= a.length; i++) {
				sum += a[i];
			}
			for (int i = 0; i + 1 < a.length; i++) {
				sum += a[i];
			}
			for (int i = 0; i < a.length; i++) {
				sum += a[i - 1];
			}
			for (int i = 0; i < b.length; i++) {
				sum += a[i];
			}
			int j = 0;
			for (int i = 0; i < a.length; j++) {
				sum += a[j];
			}
			return sum;
		}
	}

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString(":")));
	}
}
