package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumsWithAssert extends IntegrationTest {

	public static class TestCls {
		public enum Numbers {
			ONE(1), TWO(2), THREE(3);

			private final int num;

			Numbers(int n) {
				this.num = n;
			}

			public int getNum() {
				assert num > 0;
				return num;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class)).code()
				.containsOne("ONE(1)")
				.doesNotContain("Failed to restore enum class");
	}

	@NotYetImplemented("handle java assert")
	@Test
	public void testNYI() {
		assertThat(getClassNode(TestCls.class)).code()
				.containsOne("assert num > 0;")
				.doesNotContain("$assertionsDisabled")
				.doesNotContain("throw new AssertionError()");
	}
}
