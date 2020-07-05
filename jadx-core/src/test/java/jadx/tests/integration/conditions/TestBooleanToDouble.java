package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBooleanToDouble extends SmaliTest {

	// @formatter:off
	/*
		private boolean showConsent;

		public void write(double d) {
		}

		public void writeToParcel(TestBooleanToDouble testBooleanToDouble) {
			testBooleanToDouble.write(this.showConsent ? 1 : 0);
		}
	*/
	// @formatter:on
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("write(this.showConsent ? 1.0d : 0.0d);");
	}
}
