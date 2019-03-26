package jadx.tests.integration.conditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestConditions2 extends IntegrationTest {

	public static class TestCls {
		int c;
		String d;
		String f;

		public void testComplexIf(String a, int b) {
			if (d == null || (c == 0 && b != -1 && d.length() == 0)) {
				c = a.codePointAt(c);
			} else {
				if (a.hashCode() != 0xCDE) {
					c = f.compareTo(a);
				}
			}
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return;"));
		assertThat(code, not(containsString("else")));
	}
}
