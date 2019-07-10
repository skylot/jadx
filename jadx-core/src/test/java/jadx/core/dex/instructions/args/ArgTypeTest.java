package jadx.core.dex.instructions.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArgTypeTest {

	@Test
	public void testEqualsOfGenericTypes() {
		ArgType first = ArgType.generic("java.lang.List", ArgType.STRING);
		ArgType second = ArgType.generic("Ljava/lang/List;", ArgType.STRING);

		assertEquals(first, second);
	}
}
