package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCaseSensitivePkgChecks extends SmaliTest {
	/*
	 * com.Example.Foo and com.example.Foo - same class name in packages that differ only by case.
	 * On case-insensitive FS both would map to the same path (com/example/foo), requiring package
	 * rename.
	 */

	@Test
	public void testPkgConflictOnCaseInsensitiveFS() {
		args.setFsCaseSensitive(false);

		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(classes).hasSize(2);

		// all package paths must be distinct when lowercased (no two classes share same dir)
		long distinctPkgPaths = classes.stream()
				.map(cls -> cls.getClassInfo().getAliasFullPath().toLowerCase())
				.distinct()
				.count();
		assertThat(distinctPkgPaths).isEqualTo(2L);
	}

	@Test
	public void testPkgConflictOnCaseSensitiveFS() {
		args.setFsCaseSensitive(true);

		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(classes).hasSize(2);

		// on case-sensitive FS, original package names should be preserved
		long distinctPkgPaths = classes.stream()
				.map(cls -> cls.getClassInfo().getAliasFullPath())
				.distinct()
				.count();
		assertThat(distinctPkgPaths).isEqualTo(2L);
	}
}
