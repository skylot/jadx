package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

public class TestClassNameWithInvalidChar extends SmaliTest {
	/*
	 * public class do- {}
	 * public class i-f {}
	 */

	@Test
	public void test() {
		loadFromSmaliFiles();
	}

	@Test
	public void testWithDeobfuscation() {
		enableDeobfuscation();
		loadFromSmaliFiles();
	}
}
