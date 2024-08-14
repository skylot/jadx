package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
			assertThat(this.equals(other)).isTrue();

			other.b = "not-b";
			assertThat(this.equals(other)).isFalse();

			other.b = null;
			assertThat(this.equals(other)).isFalse();

			this.b = null;
			assertThat(this.equals(other)).isTrue();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2, "if (this.a != null ? this.a.equals(other.a) : other.a == null) {");
		// .containsLines(3, "if (this.b != null ? this.b.equals(other.b) : other.b == null) {")
		// .containsLines(4, "return true;")
		// .containsLines(2, "return false;")
	}

	@Test
	@NotYetImplemented
	public void testNYI() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2, "return (this.a != null ? this.a.equals(other.a) : other.a == null) "
						+ "&& (this.b == null ? other.b == null : this.b.equals(other.b));");
	}

	@Test
	public void test2() {
		getClassNodeFromSmaliWithPath("conditions", "TestTernaryInIf2");
	}
}
