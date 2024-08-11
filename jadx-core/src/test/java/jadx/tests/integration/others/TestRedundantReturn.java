package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestRedundantReturn extends IntegrationTest {

	public static class TestCls {
		public void test(int num) {
			if (num == 4) {
				fail("");
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return;");
	}
}
