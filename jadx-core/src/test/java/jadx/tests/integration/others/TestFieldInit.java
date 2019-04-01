package jadx.tests.integration.others;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("List<String> s = new ArrayList"));
		assertThat(code, containsOne("A a = new A();"));
		assertThat(code, containsOne("int i = (Random.class.getSimpleName().length() + 1);"));
		assertThat(code, containsOne("int n = 0;"));
		assertThat(code, not(containsString("static {")));
		assertThat(code, containsOne("this.n = z;"));
		assertThat(code, containsOne("this.n = 0;"));
	}
}
