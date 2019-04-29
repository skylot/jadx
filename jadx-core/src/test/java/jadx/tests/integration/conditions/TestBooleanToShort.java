package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestBooleanToShort extends SmaliTest {

	// @formatter:off
	/**
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
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToShort");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? (short) 1 : 0);"));
	}
}
