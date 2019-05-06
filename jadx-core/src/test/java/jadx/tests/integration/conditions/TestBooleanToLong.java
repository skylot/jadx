package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestBooleanToLong extends SmaliTest {

	// @formatter:off
	/**
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
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToLong");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? 1 : 0);"));
	}
}
