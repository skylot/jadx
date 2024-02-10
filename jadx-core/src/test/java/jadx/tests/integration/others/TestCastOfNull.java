package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("unused")
public class TestCastOfNull extends IntegrationTest {

	public static class TestCls {

		public void test() {
			m((long[]) null);
			m((String) null);
			m((List<String>) null);
		}

		public void m(long[] a) {
		}

		public void m(String s) {
		}

		public void m(List<String> list) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("m((long[]) null);")
				.containsOne("m((String) null);")
				.containsOne("m((List<String>) null);");
	}
}
