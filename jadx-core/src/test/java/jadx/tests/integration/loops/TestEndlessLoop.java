package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
