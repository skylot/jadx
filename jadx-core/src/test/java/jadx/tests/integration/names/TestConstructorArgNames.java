package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConstructorArgNames extends IntegrationTest {

	@SuppressWarnings({ "FieldCanBeLocal", "FieldMayBeFinal", "StaticVariableName", "ParameterName" })
	public static class TestCls {
		private static String STR = "static field";
		private final String str;
		private final String store;

		public TestCls(String str, String STR) {
			this.str = str;
			this.store = STR;
		}

		public TestCls() {
			this.str = "a";
			this.store = STR;
		}

		public void check() {
			assertThat(new TestCls("a", "b").store).isEqualTo("b");
			assertThat(new TestCls().store).isEqualTo(STR);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.str = str;")
				.containsOne("this.store = STR2;")
				.containsOne("this.store = STR;");
	}
}
