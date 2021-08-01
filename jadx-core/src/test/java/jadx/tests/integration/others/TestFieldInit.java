package jadx.tests.integration.others;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInit extends IntegrationTest {

	public static class TestCls {

		public class A {
		}

		public static List<String> s = new ArrayList<>();

		public A a = new A();
		public int i = 1 + Random.class.getSimpleName().length();
		public int n = 0;

		public TestCls(int z) {
			this.n = z;
			this.n = 0;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("List<String> s = new ArrayList")
				.containsOne("A a = new A();")
				.containsOneOf(
						"int i = (Random.class.getSimpleName().length() + 1);",
						"int i = (1 + Random.class.getSimpleName().length());")
				.containsOne("int n = 0;")
				.doesNotContain("static {")
				.containsOne("this.n = z;")
				.containsOne("this.n = 0;");
	}
}
