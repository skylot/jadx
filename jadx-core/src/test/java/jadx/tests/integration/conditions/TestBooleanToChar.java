package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToChar extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(char b) {
		}

		public void writeToParcel(TestBooleanToChar testBooleanToChar) {
			testBooleanToChar.write(this.showConsent ? (char) 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? (char) 1 : (char) 0);");
	}
}
