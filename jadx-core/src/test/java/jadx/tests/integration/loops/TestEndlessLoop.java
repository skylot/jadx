package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class TestEndlessLoop extends IntegrationTest {

	public static class TestCls {

		void test1() {
			while (this == this) {
			}
		}

		void test2() {
			do {
			} while (this == this);
		}

		void test3() {
			while (true) {
				if (this != this) {
					return;
				}
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("while (this == this)"));
	}
}
