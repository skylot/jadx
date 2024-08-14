package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatch2 extends IntegrationTest {

	public static class TestCls {
		private static final Object OBJ = new Object();

		public static boolean test() {
			try {
				synchronized (OBJ) {
					OBJ.wait(5L);
				}
				return true;
			} catch (InterruptedException e) {
				return false;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("try {")
				.contains("synchronized (OBJ) {")
				.contains("OBJ.wait(5L);")
				.contains("return true;")
				.contains("} catch (InterruptedException e) {")
				.contains("return false;");
	}
}
