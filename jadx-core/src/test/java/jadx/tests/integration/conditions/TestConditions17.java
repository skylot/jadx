package jadx.tests.integration.conditions;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestConditions17 extends IntegrationTest {

	public static class TestCls {

		public static final int SOMETHING = 2;

		public static void test(int a) {
			if ((a & SOMETHING) != 0) {
				print(1);
			}
			print(2);
		}

		public static void print(Object o) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(" & "));
	}
}
