package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTernaryInIf2 extends SmaliTest {

	public static class TestCls {
		private String a;
		private String b;

		public boolean equals(TestCls other) {
			if (this.a == null ? other.a == null : this.a.equals(other.a)) {
				if (this.b == null ? other.b == null : this.b.equals(other.b)) {
					return true;
				}
			}
			return false;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2, "if (this.a != null ? this.a.equals(other.a) : other.a == null) {"));
		assertThat(code, containsLines(3, "if (this.b != null ? this.b.equals(other.b) : other.b == null) {"));
		assertThat(code, containsLines(4, "return true;"));
		assertThat(code, containsLines(2, "return false;"));
	}

	@Test
	public void test2() {
		getClassNodeFromSmaliWithPath("conditions", "TestTernaryInIf2");
	}
}
