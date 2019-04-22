package jadx.tests.integration.conditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

public class TestBooleanToInt2 extends SmaliTest {

	/**
    	private boolean showConsent;

    	public void write(int b) {
    	}

    	public void writeToParcel(TestBooleanToInt2 testBooleanToInt2) {
        	testBooleanToInt2.write(this.showConsent ? 1 : 0);
    	}
	 */
	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("conditions", "TestBooleanToInt2");
		String code = cls.getCode().toString();

		assertThat(code, containsString("write(this.showConsent ? 1 : 0);"));
	}
}
