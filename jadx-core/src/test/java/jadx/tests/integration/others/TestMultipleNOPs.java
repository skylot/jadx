package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

public class TestMultipleNOPs extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();

		// expected no errors
		loadFromSmaliFiles();
	}
}
