package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchOverStrings6 extends SmaliTest {
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("switch (str) {")
				.containsOne("case \"alpha\":")
				.containsOne("case \"beta\":")
				.containsOne("return 10;")
				.containsOne("return 20;")
				.containsOne("return 30;");
	}
}
