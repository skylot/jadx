package jadx.tests.integration.trycatch;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally11 extends IntegrationTest {

	public static class TestCls {
		private int count = 0;

		public void test(List<Object> list) {
			try {
				call1();
			} finally {
				for (Object item : list) {
					call2(item);
				}
			}
		}

		private void call1() {
			count += 100;
		}

		private void call2(Object item) {
			count++;
		}

		public void check() {
			TestCls t = new TestCls();
			t.test(Arrays.asList("1", "2"));
			assertThat(t.count).isEqualTo(102);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} finally {");
	}
}
