package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariables6 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmaliWithPath("variables", "TestVariables6"))
				.code()
				.doesNotContain("r4")
				.doesNotContain("r1v1")
				.contains("DateStringParser dateStringParser")
				.contains("FinancialInstrumentMetadataAttribute startYear ="
						+ " this.mFinancialInstrumentMetadataDefinition.getStartYear();");
	}
}
