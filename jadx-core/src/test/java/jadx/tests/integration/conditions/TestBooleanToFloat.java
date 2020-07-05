package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToFloat extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(float f) {
		}

		public void writeToParcel(TestBooleanToFloat testBooleanToFloat) {
			testBooleanToFloat.write(this.showConsent ? 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? 1.0f : 0.0f);");
	}
}
