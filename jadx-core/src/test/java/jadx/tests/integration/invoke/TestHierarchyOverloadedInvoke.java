package jadx.tests.integration.invoke;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestHierarchyOverloadedInvoke extends IntegrationTest {

	public static class TestCls {
		static int c = 0;
		B b = new B();

		public interface I {
			default void call(String str) {
				c += 1;
			}
		}

		public static class A implements I {
			public void call(List<String> list) {
				c += 10;
			}
		}

		public static class B extends A {
			public void call(ArrayList<String> list) {
				c += 100;
			}
		}

		public void test() {
			b.call(new ArrayList<>());
			b.call((List<String>) new ArrayList<String>());
		}

		public void test2(Object obj) {
			if (obj instanceof String) {
				b.call((String) obj);
			}
		}

		public void test3() {
			b.call((String) null);
			b.call((List<String>) null);
			b.call((ArrayList<String>) null);
		}

		public void test4() {
			((I) b).call(null);
			((A) b).call((String) null);
			((A) b).call((List<String>) null);
		}

		public void check() {
			test();
			assertThat(c, is(10 + 100));

			c = 0;
			test2("str");
			assertThat(c, is(1));

			c = 0;
			test3();
			assertThat(c, is(111));

			c = 0;
			test4();
			assertThat(c, is(12));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("b.call(new ArrayList<>());"));
		assertThat(code, containsOne("b.call((List<String>) new ArrayList());"));

		assertThat(code, containsOne("b.call((String) obj);"));
	}
}
