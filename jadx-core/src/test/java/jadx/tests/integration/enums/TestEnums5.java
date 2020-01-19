package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums5 extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithClsName("kotlin.collections.State"))
				.code()
				.containsLines(
						"enum State {",
						indent() + "Ready,",
						indent() + "NotReady,",
						indent() + "Done,",
						indent() + "Failed",
						"}");
	}
}
