package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums11 extends RaungTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsLines("public enum TestEnums11 {", indent(1) + "UNKNOWN;")
				.containsOne("public final int a = -99;")
				.doesNotContain("TestEnums11() {");
	}

	@Test
	public void testDisableEnumRestore() {
		// constructor method incorrectly removed
		getArgs().getDisabledPasses().add("EnumVisitor");
		disableCompilation();
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("public TestEnums11() {");
	}
}
