package jadx.tests.integration.invoke;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCastInOverloadedInvoke extends IntegrationTest {

	@SuppressWarnings("IllegalType")
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
			assertThat(c).isEqualTo(10 + 100);
			c = 0;
			test2("str");
			assertThat(c).isEqualTo(1);
			c = 0;
			test3();
			assertThat(c).isEqualTo(111);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("call(new ArrayList<>());")
				.containsOne("call((List<String>) new ArrayList());")
				.containsOne("call((String) obj);");
	}

	@NotYetImplemented
	@Test
	public void testNYI() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("call((List<String>) new ArrayList<String>());");
	}
}
