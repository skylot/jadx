package jadx.tests.integration.names;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCaseSensitiveClassInPkgChecks extends SmaliTest {
	/*
	 * com.example.User and com.example.user - class names differ only by case in the same package.
	 * On case-insensitive FS both would map to the same file path, requiring class rename.
	 */

	@Test
	public void testClassConflictOnCaseInsensitiveFS() {
		args.setFsCaseSensitive(false);

		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(classes).hasSize(2);

		long distinct = classes.stream()
				.map(cls -> cls.getClassInfo().getAliasFullPath().toLowerCase())
				.distinct()
				.count();
		assertThat(distinct).isEqualTo(2L);
	}

	@Test
	public void testClassConflictOnCaseSensitiveFS() {
		args.setFsCaseSensitive(true);

		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(classes).hasSize(2);

		long distinct = classes.stream()
				.map(cls -> cls.getClassInfo().getAliasFullPath())
				.distinct()
				.count();
		assertThat(distinct).isEqualTo(2L);
	}
}
