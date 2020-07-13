package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInlineVarArg extends SmaliTest {

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("f(\"a\", \"b\", \"c\");");
	}
}
