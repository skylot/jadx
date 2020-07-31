package jadx.core.utils;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class TypeUtilsTest {
	private static final Logger LOG = LoggerFactory.getLogger(TypeUtilsTest.class);

	private static RootNode root;

	@BeforeAll
	public static void init() {
		root = new RootNode(new JadxArgs());
		root.initClassPath();
	}

	@Test
	public void testReplaceGenericsWithWildcards() {
		// check classpath graph
		List<ArgType> classGenerics = root.getTypeUtils().getClassGenerics(ArgType.object("java.util.ArrayList"));
		assertThat(classGenerics, hasSize(1));
		ArgType genericInfo = classGenerics.get(0);
		assertThat(genericInfo.getObject(), is("E"));
		assertThat(genericInfo.getExtendTypes(), hasSize(0));

		// prepare input
		ArgType instanceType = ArgType.generic("java.util.ArrayList", ArgType.OBJECT);
		LOG.debug("instanceType: {}", instanceType);

		ArgType generic = ArgType.generic("java.util.List", ArgType.wildcard(ArgType.genericType("E"), ArgType.WildcardBound.SUPER));
		LOG.debug("generic: {}", generic);

		// replace
		ArgType result = root.getTypeUtils().replaceClassGenerics(instanceType, generic);
		LOG.debug("result: {}", result);

		ArgType expected = ArgType.generic("java.util.List", ArgType.wildcard(ArgType.OBJECT, ArgType.WildcardBound.SUPER));
		assertThat(result, is(expected));
	}
}
