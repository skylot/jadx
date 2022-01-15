package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToShort extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(short b) {
		}

		public void writeToParcel(TestBooleanToShort testBooleanToShort) {
			testBooleanToShort.write(this.showConsent ? (short) 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? (short) 1 : (short) 0);");
	}
}
