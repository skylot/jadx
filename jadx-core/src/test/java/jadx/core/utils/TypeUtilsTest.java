package jadx.core.utils;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.GenericInfo;
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
		root.load(Collections.emptyList());
		root.initClassPath();
	}

	@Test
	public void testReplaceGenericsWithWildcards() {
		// check classpath graph
		List<GenericInfo> classGenerics = root.getClassGenerics(ArgType.object("java.util.ArrayList"));
		assertThat(classGenerics, hasSize(1));
		GenericInfo genericInfo = classGenerics.get(0);
		assertThat(genericInfo.getGenericType(), is(ArgType.genericType("E")));
		assertThat(genericInfo.getExtendsList(), hasSize(0));

		// prepare input
		ArgType instanceType = ArgType.generic("java.util.ArrayList", ArgType.OBJECT);
		LOG.debug("instanceType: {}", instanceType);

		ArgType generic = ArgType.generic("java.util.List", ArgType.wildcard(ArgType.genericType("E"), ArgType.WildcardBound.SUPER));
		LOG.debug("generic: {}", generic);

		// replace
		ArgType result = TypeUtils.replaceClassGenerics(root, instanceType, generic);
		LOG.debug("result: {}", result);

		ArgType expected = ArgType.generic("java.util.List", ArgType.wildcard(ArgType.OBJECT, ArgType.WildcardBound.SUPER));
		assertThat(result, is(expected));
	}
}
