package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayForEachNegative extends IntegrationTest {

	public static class TestCls {

		public int test(int[] a, int[] b) {
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
		// Remove all comments - as the comment created by CodeGenUtils.addInputFileInfo always contains a
		// colon
		getArgs().setCommentsLevel(CommentsLevel.NONE);
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain(":");
	}
}
