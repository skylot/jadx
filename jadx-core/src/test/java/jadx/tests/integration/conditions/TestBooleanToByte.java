package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToByte extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(byte b) {
		}

		public void writeToParcel(TestBooleanToByte testBooleanToByte) {
			testBooleanToByte.write(this.showConsent ? (byte) 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? (byte) 1 : (byte) 0);");
	}
}
