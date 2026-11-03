package jadx.commons.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dirs.ProjectDirectories;
import dev.dirs.impl.Windows;

public class JadxCommonFiles {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCommonFiles.class);

	private static final String ORG_DIR_NAME = "skylot";
	private static final String APP_DIR_NAME = "jadx";

	/**
	 * 'FOLDERID_RoamingAppData', requested by 'dev.dirs' to build config dir path
	 */
	private static final String FOLDERID_ROAMING_APP_DATA = "3EB685DB-65F9-4CF6-A03A-E3EF65729F3D";
	/**
	 * 'FOLDERID_LocalAppData', requested by 'dev.dirs' to build cache dir path
	 */
	private static final String FOLDERID_LOCAL_APP_DATA = "F1B32785-6FBA-4FCF-9D55-7B8E7F157091";

	private static final Path CONFIG_DIR;
	private static final Path CACHE_DIR;

	public static Path getConfigDir() {
		return CONFIG_DIR;
	}

	public static Path getCacheDir() {
		return CACHE_DIR;
	}

	static {
		DirsLoader loader = new DirsLoader();
		CONFIG_DIR = loader.getConfigDir();
		CACHE_DIR = loader.getCacheDir();
	}

	private static final class DirsLoader {
		private final Path configDir;
		private final Path cacheDir;

		DirsLoader() {
			try {
				long start = System.currentTimeMillis();
				ProjectDirectories dirs = ProjectDirectories.from("io.github", ORG_DIR_NAME, APP_DIR_NAME,
						DirsLoader::getWinDirs);
				LOG.debug("Loaded system dirs ({}ms): config: {}, cache: {}",
						System.currentTimeMillis() - start, dirs.configDir, dirs.cacheDir);
				configDir = prepareDir(JadxCommonEnv.get("JADX_CONFIG_DIR", dirs.configDir));
				cacheDir = prepareDir(JadxCommonEnv.get("JADX_CACHE_DIR", dirs.cacheDir));
			} catch (Exception e) {
				throw new RuntimeException("Failed to init common directories", e);
			}
		}

		private static Path prepareDir(String dir) throws IOException {
			Path path = Path.of(dir).toAbsolutePath();
			Files.createDirectories(path);
			return path;
		}

		private static Windows getWinDirs() {
			return EnvWinDirs.INSTANCE;
		}

		Path getCacheDir() {
			return cacheDir;
		}

		Path getConfigDir() {
			return configDir;
		}
	}

	/**
	 * Resolve known folders on Windows using environment variables.
	 * <p>
	 * Default implementation returned by {@link Windows#getDefaultSupplier()} uses JNI or an
	 * external process ('cmd' or 'powershell'). If such a process doesn't finish (blocked by
	 * a policy, for example), jadx hangs on startup without any error and without a window.
	 * Both folders requested by 'dev.dirs' are always available as environment variables,
	 * so they can be read directly. Everything else (project path, 'config' and 'cache'
	 * sub directories) is still done by 'dev.dirs', so paths stay the same as on other systems.
	 */
	private static final class EnvWinDirs implements Windows {
		private static final EnvWinDirs INSTANCE = new EnvWinDirs();

		@Override
		public String[] winDirs(String... guids) {
			String[] paths = new String[guids.length];
			for (int i = 0; i < guids.length; i++) {
				paths[i] = getKnownFolder(guids[i]);
			}
			return paths;
		}

		private static String getKnownFolder(String guid) {
			switch (guid) {
				case FOLDERID_ROAMING_APP_DATA:
					return envDir("APPDATA", "Roaming");
				case FOLDERID_LOCAL_APP_DATA:
					return envDir("LOCALAPPDATA", "Local");
				default:
					throw new IllegalArgumentException("Unexpected known folder id: " + guid);
			}
		}

		private static String envDir(String envVar, String appDataSubDir) {
			String dir = JadxCommonEnv.get(envVar, null);
			if (dir != null) {
				return dir;
			}
			return Path.of(System.getProperty("user.home", "."), "AppData", appDataSubDir).toString();
		}
	}

	private JadxCommonFiles() {
	}
}
