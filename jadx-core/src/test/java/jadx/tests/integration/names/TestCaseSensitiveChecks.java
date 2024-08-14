package jadx.tests.integration.names;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jadx.core.Consts;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
			assertThat(cls.getPackage()).isEqualTo(Consts.DEFAULT_PACKAGE_NAME);
		}
		long namesCount = classes.stream().map(cls -> cls.getAlias().toLowerCase()).distinct().count();
		assertThat(namesCount).isEqualTo(2L);
	}

	@Test
	public void testCaseSensitiveFS() {
		args.setFsCaseSensitive(true);

		List<ClassNode> classes = loadFromSmaliFiles();
		for (ClassNode cls : classes) {
			assertThat(cls.getPackage()).isEqualTo(Consts.DEFAULT_PACKAGE_NAME);
		}
		List<String> names = classes.stream().map(ClassNode::getAlias).collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("A", "a");
	}

	@Test
	public void testWithDeobfuscation() {
		enableDeobfuscation();

		List<ClassNode> classes = loadFromSmaliFiles();
		for (ClassNode cls : classes) {
			assertThat(cls.getPackage()).isNotEmpty();
			assertThat(cls.getPackage()).isNotEqualTo(Consts.DEFAULT_PACKAGE_NAME);
		}
		long namesCount = classes.stream().map(cls -> cls.getAlias().toLowerCase()).distinct().count();
		assertThat(namesCount).isEqualTo(2L);
	}
}
