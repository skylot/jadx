package jadx.tests.integration.conditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestBooleanToByte extends SmaliTest {

	/**
    	private boolean showConsent;

    	public void writeByte(byte b) {
    	}

    	public void writeToParcel(TestBooleanToByte testBooleanToByte, int i) {
        	testBooleanToByte.writeByte(this.showConsent ? (byte) 1 : 0);
    	}

	 */
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToByte");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? (byte) 1 : 0);"));
	}
}
