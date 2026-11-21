package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop10 extends IntegrationTest {

	public static class TestCls {
		private int pos;

		public TestCls mergeFrom() {
			int tag;
			do {
				tag = readTag();
				switch (tag) {
					case 0:
						return this;
					default:
						break;
				}
			} while (parseUnknownField(tag));
			return this;
		}

		private int readTag() {
			return pos++ == 0 ? 1 : 0;
		}

		private boolean parseUnknownField(int tag) {
			return tag > 0 && pos < 3;
		}

		public void check() {
			org.assertj.core.api.Assertions.assertThat(mergeFrom()).isSameAs(this);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("do {")
				.containsOne("switch (tag) {")
				.containsOne("case 0:")
				.containsOne("while (parseUnknownField(tag));")
				.doesNotContain("Switch 'out' block");
	}
}
