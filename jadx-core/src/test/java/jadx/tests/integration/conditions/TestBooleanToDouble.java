package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestBooleanToDouble extends SmaliTest {

	// @formatter:off
	/**
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
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToDouble");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? 1.0d : 0.0d);"));
	}
}
