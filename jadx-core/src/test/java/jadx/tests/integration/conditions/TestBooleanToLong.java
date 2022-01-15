package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToLong extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(long j) {
		}

		public void writeToParcel(TestBooleanToLong testBooleanToLong) {
			testBooleanToLong.write(this.showConsent ? 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? 1L : 0L);");
	}
}
