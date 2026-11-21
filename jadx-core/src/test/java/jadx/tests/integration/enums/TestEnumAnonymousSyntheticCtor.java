package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumAnonymousSyntheticCtor extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliFiles())
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("Enum visitor error")
				.containsOne("public enum TestEnumAnonymousSyntheticCtor {")
				.containsOne("TEST1(\"TEST1\", 0) {")
				.containsOne("public String toString() {");
	}
}
