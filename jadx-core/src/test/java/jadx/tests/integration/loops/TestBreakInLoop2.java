package jadx.tests.integration.loops;

import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestBreakInLoop2 extends IntegrationTest {

	public static class TestCls {
		public void test(List<Integer> data) throws Exception {
			for (; ; ) {
				try {
					funcB(data);
					break;
				} catch (Exception ex) {
					if (funcC()) {
						throw ex;
					}
					data.clear();
				}
				Thread.sleep(100);
			}
		}

		private boolean funcB(List<Integer> data) {
			return false;
		}

		private boolean funcC() {
			return true;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while (true) {"));
		assertThat(code, containsOne("break;"));
		assertThat(code, containsOne("throw ex;"));
		assertThat(code, containsOne("data.clear();"));
		assertThat(code, containsOne("Thread.sleep(100);"));
	}
}
