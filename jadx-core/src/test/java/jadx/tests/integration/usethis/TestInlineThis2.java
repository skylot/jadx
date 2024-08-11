package jadx.tests.integration.usethis;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInlineThis2 extends IntegrationTest {

	@SuppressWarnings("ConstantValue")
	public static class TestCls {
		public int field;

		public void test() {
			TestCls thisVar = this;
			if (Objects.isNull(thisVar)) {
				System.out.println("null");
			}
			thisVar.method();
			thisVar.field = 123;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("thisVar")
				.doesNotContain("thisVar.method()")
				.doesNotContain("thisVar.field")
				.doesNotContain("= this")
				.containsOne("if (Objects.isNull(this)) {")
				.containsOne("this.field = 123;")
				.containsOne("method();");
	}
}
