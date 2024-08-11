package jadx.core.dex.instructions.args;

import org.junit.jupiter.api.Test;

import static jadx.core.dex.instructions.args.ArgType.WildcardBound.SUPER;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.wildcard;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class ArgTypeTest {

	@Test
	public void testEqualsOfGenericTypes() {
		ArgType first = ArgType.generic("java.lang.List", ArgType.STRING);
		ArgType second = ArgType.generic("Ljava/lang/List;", ArgType.STRING);

		assertThat(first).isEqualTo(second);
	}

	@Test
	void testContainsGenericType() {
		ArgType wildcard = wildcard(genericType("T"), SUPER);
		assertThat(wildcard.containsTypeVariable()).isTrue();

		ArgType type = generic("java.lang.List", wildcard);
		assertThat(type.containsTypeVariable()).isTrue();
	}

	@Test
	void testInnerGeneric() {
		ArgType[] genericTypes = new ArgType[] { ArgType.genericType("K"), ArgType.genericType("V") };
		ArgType base = ArgType.generic("java.util.Map", genericTypes);

		ArgType genericInner = ArgType.outerGeneric(base, ArgType.generic("Entry", genericTypes));
		assertThat(genericInner.toString()).isEqualTo("java.util.Map<K, V>$Entry<K, V>");
		assertThat(genericInner.containsTypeVariable()).isTrue();

		ArgType genericInner2 = ArgType.outerGeneric(base, ArgType.object("Entry"));
		assertThat(genericInner2.toString()).isEqualTo("java.util.Map<K, V>$Entry");
		assertThat(genericInner2.containsTypeVariable()).isTrue();
	}
}
