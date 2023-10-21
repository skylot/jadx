package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

public class TestTernaryInIf3 extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		getClassNodeFromSmali();
	}
}
