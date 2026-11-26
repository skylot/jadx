package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// issue #2882
public class TestSwitch5 extends SmaliTest {
	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("if (i != -1 && i != 0 && i != 1) {")
				.countString(2, "switch (i) {")
				.containsOne("case 30:")
				.containsOne("case 40:")
				.containsOne("System.out.println(i);");
	}
}
