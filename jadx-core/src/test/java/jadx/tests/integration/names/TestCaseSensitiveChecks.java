package jadx.tests.integration.names;

import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jadx.core.Consts;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestCaseSensitiveChecks extends SmaliTest {
	/*
	 * public class A {}
	 * public class a {}
	 */

	@Test
	public void test() {
		args.setFsCaseSensitive(false);

		List<ClassNode> classes = loadFromSmaliFiles();
		for (ClassNode cls : classes) {
			assertThat(cls.getPackage(), is(Consts.DEFAULT_PACKAGE_NAME));
		}
		long namesCount = classes.stream().map(cls -> cls.getAlias().toLowerCase()).distinct().count();
		assertThat(namesCount, is(2L));
	}

	@Test
	public void testCaseSensitiveFS() {
		args.setFsCaseSensitive(true);

		List<ClassNode> classes = loadFromSmaliFiles();
		for (ClassNode cls : classes) {
			assertThat(cls.getPackage(), is(Consts.DEFAULT_PACKAGE_NAME));
		}
		List<String> names = classes.stream().map(ClassNode::getAlias).collect(Collectors.toList());
		assertThat(names, Matchers.containsInAnyOrder("A", "a"));
	}

	@Test
	public void testWithDeobfuscation() {
		enableDeobfuscation();

		List<ClassNode> classes = loadFromSmaliFiles();
		for (ClassNode cls : classes) {
			assertThat(cls.getPackage(), not(emptyString()));
			assertThat(cls.getPackage(), not(Consts.DEFAULT_PACKAGE_NAME));
		}
		long namesCount = classes.stream().map(cls -> cls.getAlias().toLowerCase()).distinct().count();
		assertThat(namesCount, is(2L));
	}
}
