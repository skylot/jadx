package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions17 extends IntegrationTest {

	public static class TestCls {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a | b) != false) {
				test();
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test202() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("a || b"));
	}
}
