package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstStringConcat extends IntegrationTest {

	@SuppressWarnings("StringBufferReplaceableByString")
	public static class TestCls {
		public String test1(int value) {
			return new StringBuilder().append("Value").append(" equals ").append(value).toString();
		}

		public String test2() {
			return new StringBuilder().append("App ").append("version: ").append(1).append('.').append(2).toString();
		}

		public String test3(String name, int value) {
			return "value " + name + " = " + value;
		}

		public void check() {
			assertThat(test1(7)).isEqualTo("Value equals 7");
			assertThat(test2()).isEqualTo("App version: 1.2");
			assertThat(test3("v", 4)).isEqualTo("value v = 4");
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return \"Value equals \" + ")
				.containsOne("return \"App version: 1.2\";")
				.containsOne("return \"value \" + str + \" = \" + i;");
	}
}
