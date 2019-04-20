package jadx.tests.integration.conditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestBooleanToChar extends SmaliTest {

	/**
    	private boolean showConsent;

    	public void write(char b) {
    	}

    	public void writeToParcel(TestBooleanToChar testBooleanToChar) {
        	testBooleanToChar.write(this.showConsent ? (char) 1 : 0);
    	}
	 */
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToChar");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? (char) 1 : 0);"));
	}
}
