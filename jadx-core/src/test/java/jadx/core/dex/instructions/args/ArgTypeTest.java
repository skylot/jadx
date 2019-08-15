package jadx.core.dex.instructions.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgTypeTest {

	@Test
	public void testEqualsOfGenericTypes() {
		ArgType first = ArgType.generic("java.lang.List", ArgType.STRING);
		ArgType second = ArgType.generic("Ljava/lang/List;", ArgType.STRING);

		assertEquals(first, second);
	}

	@Test
	void testContainsGenericType() {
		ArgType wildcard = ArgType.wildcard(ArgType.genericType("T"), ArgType.WildcardBound.SUPER);
		assertTrue(wildcard.containsGenericType());

		ArgType type = ArgType.generic("java.lang.List", wildcard);
		assertTrue(type.containsGenericType());

	}
}
