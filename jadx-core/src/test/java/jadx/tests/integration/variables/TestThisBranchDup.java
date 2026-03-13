package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestThisBranchDup extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("this(") // constructor type correctly detected
				.countString(6, "(i & "); // ternary used and inlined
	}
}
