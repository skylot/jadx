package jadx.tests.integration.invoke;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestCastInOverloadedInvoke extends IntegrationTest {

	public static class TestCls {
		int c = 0;

		public void test() {
			call(new ArrayList<>());
			call((List<String>) new ArrayList<String>());
		}

		public void test2(Object obj) {
			if (obj instanceof String) {
				call((String) obj);
			}
		}

		public void test3() {
			call((String) null);
			call((List<String>) null);
			call((ArrayList<String>) null);
		}

		public void call(String str) {
			c += 1;
		}

		public void call(List<String> list) {
			c += 10;
		}

		public void call(ArrayList<String> list) {
			c += 100;
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
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("call(new ArrayList<>());"));
		assertThat(code, containsOne("call((List<String>) new ArrayList());"));

		assertThat(code, containsOne("call((String) obj);"));
	}

	@NotYetImplemented
	@Test
	public void testNYI() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("call((List<String>) new ArrayList<String>());"));
	}
}
