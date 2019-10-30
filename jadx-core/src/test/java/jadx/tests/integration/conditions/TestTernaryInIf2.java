package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestTernaryInIf2 extends SmaliTest {

	public static class TestCls {
		private String a = "a";
		private String b = "b";

		public boolean equals(TestCls other) {
			if (this.a == null ? other.a == null : this.a.equals(other.a)) {
				if (this.b == null ? other.b == null : this.b.equals(other.b)) {
					return true;
				}
			}
			return false;
		}

		public void check() {
			TestCls other = new TestCls();
			other.a = "a";
			other.b = "b";
			assertThat(this.equals(other), is(true));

			other.b = "not-b";
			assertThat(this.equals(other), is(false));

			other.b = null;
			assertThat(this.equals(other), is(false));

			this.b = null;
			assertThat(this.equals(other), is(true));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2, "if (this.a != null ? this.a.equals(other.a) : other.a == null) {"));
		// assertThat(code, containsLines(3, "if (this.b != null ? this.b.equals(other.b) : other.b == null)
		// {"));
		// assertThat(code, containsLines(4, "return true;"));
		// assertThat(code, containsLines(2, "return false;"));
	}

	@Test
	@NotYetImplemented
	public void testNYI() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2, "return (this.a != null ? this.a.equals(other.a) : other.a == null) "
				+ "&& (this.b == null ? other.b == null : this.b.equals(other.b));"));
	}

	@Test
	public void test2() {
		getClassNodeFromSmaliWithPath("conditions", "TestTernaryInIf2");
	}
}
