package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestVariables6 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		ClassNode cls = getClassNodeFromSmaliWithPath("variables", "TestVariables6");
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("r4")));
		assertThat(code, not(containsString("r1v1")));
		assertThat(code, containsString("DateStringParser dateStringParser"));
		assertThat(code, containsString("FinancialInstrumentMetadataAttribute startYear ="
				+ " this.mFinancialInstrumentMetadataDefinition.getStartYear();"));
	}
}
