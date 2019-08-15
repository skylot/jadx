package jadx.core.dex.instructions.args;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgTypeTest {

	private static final Logger LOG = LoggerFactory.getLogger(ArgTypeTest.class);

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

	@Test
	void testInnerGeneric() {
		ArgType[] genericTypes = new ArgType[] { ArgType.genericType("K"), ArgType.genericType("V") };
		ArgType base = ArgType.generic("java.util.Map", genericTypes);

		ArgType genericInner = ArgType.outerGeneric(base, ArgType.generic("Entry", genericTypes));
		LOG.debug("genericInner : {}", genericInner);

		ArgType genericInner2 = ArgType.outerGeneric(base, ArgType.object("Entry"));
		LOG.debug("genericInner2: {}", genericInner2);

		assertThat(genericInner.toString(), is("java.util.Map<K, V>$Entry<K, V>"));
	}
}
