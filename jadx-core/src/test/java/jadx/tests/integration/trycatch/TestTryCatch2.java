package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("synchronized (OBJ) {"));
		assertThat(code, containsString("OBJ.wait(5L);"));
		assertThat(code, containsString("return true;"));
		assertThat(code, containsString("} catch (InterruptedException e) {"));
		assertThat(code, containsString("return false;"));
	}
}
