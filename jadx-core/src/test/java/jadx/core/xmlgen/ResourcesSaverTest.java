package jadx.core.xmlgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResourcesSaverTest {

	@Test
	void testNoConflictForNewFile(@TempDir Path dir) {
		Path manifest = dir.resolve("AndroidManifest.xml");
		assertThat(ResourcesSaver.hasNameConflict(manifest)).isFalse();
		assertThat(ResourcesSaver.resolveNameConflict(manifest)).isEqualTo(manifest);
	}

	@Test
	void testNoConflictForSameName(@TempDir Path dir) throws IOException {
		Path manifest = dir.resolve("AndroidManifest.xml");
		Files.writeString(manifest, "<manifest/>");
		// same name is an expected overwrite
		assertThat(ResourcesSaver.hasNameConflict(manifest)).isFalse();
		assertThat(ResourcesSaver.resolveNameConflict(manifest)).isEqualTo(manifest);
	}

	@Test
	void testConflictForNameWithDifferentCase(@TempDir Path dir) throws IOException {
		assumeCaseInsensitiveFileSystem(dir);
		Files.writeString(dir.resolve("AndroidManifest.xml"), "<manifest/>");

		Path upperCase = dir.resolve("ANDROIDMANIFEST.XML");
		assertThat(ResourcesSaver.hasNameConflict(upperCase)).isTrue();
		assertThat(ResourcesSaver.resolveNameConflict(upperCase))
				.isEqualTo(dir.resolve("ANDROIDMANIFEST_1.XML"));
	}

	@Test
	void testConflictSkipsAlreadyUsedName(@TempDir Path dir) throws IOException {
		assumeCaseInsensitiveFileSystem(dir);
		Files.writeString(dir.resolve("AndroidManifest.xml"), "<manifest/>");
		Files.writeString(dir.resolve("ANDROIDMANIFEST_1.XML"), "<manifest/>");

		assertThat(ResourcesSaver.resolveNameConflict(dir.resolve("ANDROIDMANIFEST.XML")))
				.isEqualTo(dir.resolve("ANDROIDMANIFEST_2.XML"));
	}

	/**
	 * Case sensitivity is a property of a file system (or even of a directory on Windows),
	 * so check it instead of relying on an OS name.
	 */
	private static void assumeCaseInsensitiveFileSystem(Path dir) throws IOException {
		Path probe = Files.createFile(dir.resolve("case-sensitivity-probe.tmp"));
		try {
			assumeTrue(Files.exists(dir.resolve("CASE-SENSITIVITY-PROBE.TMP")),
					"File system is case sensitive, nothing to check");
		} finally {
			Files.deleteIfExists(probe);
		}
	}
}
