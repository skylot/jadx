package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArithConst extends SmaliTest {

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNodeFromSmaliWithPath("arith", "TestArithConst"))
				.code()
				.containsOne("return i + CONST_INT;");
	}
}
