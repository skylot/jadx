package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldAccess extends IntegrationTest {

	public static class TestCls {
		private String field;

		static <T extends TestCls> T testPut(T t) {
			((TestCls) t).field = "";
			return t;
		}

		static <T extends TestCls> T testGet(T t) {
			System.out.println(((TestCls) t).field);
			return t;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("t.field");
	}
}
