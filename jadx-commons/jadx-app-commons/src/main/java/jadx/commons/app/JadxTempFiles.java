package jadx.commons.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JadxTempFiles {
	private static final String JADX_TMP_INSTANCE_PREFIX = "jadx-instance-";

	private static final Path TEMP_ROOT_DIR = createTempRootDir();

	public static Path getTempRootDir() {
		return TEMP_ROOT_DIR;
	}

	private static Path createTempRootDir() {
		try {
			String jadxTmpDir = System.getenv("JADX_TMP_DIR");
			Path dir;
			if (jadxTmpDir != null) {
				dir = Files.createTempDirectory(Paths.get(jadxTmpDir), JADX_TMP_INSTANCE_PREFIX);
			} else {
				dir = Files.createTempDirectory(JADX_TMP_INSTANCE_PREFIX);
			}
			dir.toFile().deleteOnExit();
			return dir;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create temp root directory", e);
		}
	}
}
