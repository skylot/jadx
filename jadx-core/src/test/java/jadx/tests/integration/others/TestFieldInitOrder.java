package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitOrder extends IntegrationTest {

	public static class TestCls {
		private final StringBuilder sb = new StringBuilder();
		private final String a = sb.append("a").toString();
		private final String b = sb.append("b").toString();
		private final String c = sb.append("c").toString();
		private final String result = sb.toString();

		public void check() {
			assertThat(result).isEqualTo("abc");
			assertThat(a).isEqualTo("a");
			assertThat(b).isEqualTo("ab");
			assertThat(c).isEqualTo("abc");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("TestCls() {") // constructor removed
				.doesNotContain("String result;")
				.containsOne("String result = this.sb.toString();");
	}
}
