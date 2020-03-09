package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumsWithStaticFields extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOnlyOnce("INSTANCE;")
				.containsOnlyOnce("private static c sB")
				.doesNotContain(" sA")
				.doesNotContain(" sC")
				.doesNotContain("private TestEnumsWithStaticFields(String str) {");
	}
}
