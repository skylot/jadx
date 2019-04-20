package jadx.tests.integration.conditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestBooleanToShort extends SmaliTest {

	/**
    	private boolean showConsent;

    	public void write(short b) {
    	}

    	public void writeToParcel(TestBooleanToShort testBooleanToShort) {
        	testBooleanToShort.write(this.showConsent ? (short) 1 : 0);
    	}
	 */
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToShort");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? (short) 1 : 0);"));
	}
}
