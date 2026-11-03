package jadx.commons.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dirs.ProjectDirectories;
import dev.dirs.impl.Windows;
import dev.dirs.impl.WindowsPowerShell;
import dev.dirs.jni.WindowsJni;

public class JadxCommonFiles {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCommonFiles.class);

	private static final String ORG_DIR_NAME = "skylot";
	private static final String APP_DIR_NAME = "jadx";

	private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	private static final boolean IS_WINDOWS = OS_NAME.startsWith("windows");
	private static final boolean IS_MAC = OS_NAME.contains("mac");

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
				String configDirEnv = JadxCommonEnv.get("JADX_CONFIG_DIR", null);
				String cacheDirEnv = JadxCommonEnv.get("JADX_CACHE_DIR", null);
				ProjectDirectories dirs = null;
				if (configDirEnv == null || cacheDirEnv == null) {
					dirs = loadDirs();
				}
				configDir = prepareDir(configDirEnv != null ? configDirEnv
						: dirs != null ? dirs.configDir : defaultConfigDir());
				cacheDir = prepareDir(cacheDirEnv != null ? cacheDirEnv
						: dirs != null ? dirs.cacheDir : defaultCacheDir());
			} catch (Exception e) {
				throw new RuntimeException("Failed to init common directories", e);
			}
		}

		private static Path prepareDir(String dir) throws IOException {
			Path path = Path.of(dir).toAbsolutePath();
			Files.createDirectories(path);
			return path;
		}

		/**
		 * Load system dirs.
		 * Return null if not possible, so caller can use default locations.
		 */
		private static @Nullable ProjectDirectories loadDirs() {
			if (IS_WINDOWS) {
				/*
				 * On Windows 'dev.dirs' resolves known folders by running 'cmd' or 'powershell'
				 * (when JNI is not available) and reading their output. If such a process doesn't
				 * finish, jadx hangs on startup without any error. Locations returned by 'dev.dirs'
				 * on Windows are just '%APPDATA%' and '%LOCALAPPDATA%' based, so use them directly.
				 */
				LOG.debug("Skip system dirs loading on Windows, use environment variables");
				return null;
			}
			LOG.debug("Loading system dirs ...");
			long start = System.currentTimeMillis();

			ProjectDirectories loadedDirs = ProjectDirectories.from("io.github", ORG_DIR_NAME, APP_DIR_NAME,
					DirsLoader::getWinDirs);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Loaded system dirs ({}ms): config: {}, cache: {}",
						System.currentTimeMillis() - start, loadedDirs.configDir, loadedDirs.cacheDir);
			}
			return loadedDirs;
		}

		/**
		 * Same location as returned by 'dev.dirs', used when it is not available.
		 */
		private static String defaultConfigDir() {
			if (IS_WINDOWS) {
				return defaultWinAppDir("APPDATA", "Roaming").resolve("config").toString();
			}
			if (IS_MAC) {
				return Path.of(userHome(), "Library", "Application Support", APP_DIR_NAME).toString();
			}
			String xdgDir = JadxCommonEnv.get("XDG_CONFIG_HOME", null);
			return (xdgDir != null ? Path.of(xdgDir) : Path.of(userHome(), ".config")).resolve(APP_DIR_NAME).toString();
		}

		/**
		 * Same location as returned by 'dev.dirs', used when it is not available.
		 */
		private static String defaultCacheDir() {
			if (IS_WINDOWS) {
				return defaultWinAppDir("LOCALAPPDATA", "Local").resolve("cache").toString();
			}
			if (IS_MAC) {
				return Path.of(userHome(), "Library", "Caches", APP_DIR_NAME).toString();
			}
			String xdgDir = JadxCommonEnv.get("XDG_CACHE_HOME", null);
			return (xdgDir != null ? Path.of(xdgDir) : Path.of(userHome(), ".cache")).resolve(APP_DIR_NAME).toString();
		}

		private static Path defaultWinAppDir(String envVar, String appDataSubDir) {
			String appDataDir = JadxCommonEnv.get(envVar, null);
			Path baseDir = appDataDir != null
					? Path.of(appDataDir)
					: Path.of(userHome(), "AppData", appDataSubDir);
			return baseDir.resolve(ORG_DIR_NAME).resolve(APP_DIR_NAME);
		}

		private static String userHome() {
			return System.getProperty("user.home", ".");
		}

		/**
		 * Return JNI, Foreign or PowerShell implementation
		 */
		private static Windows getWinDirs() {
			Windows impl = Windows.getDefaultSupplier().get();
			if (impl instanceof WindowsPowerShell) {
				if (JadxSystemInfo.IS_AMD64) {
					// JNI library compiled only for x86-64
					impl = new WindowsJni();
				}
			}
			LOG.debug("Using win dirs implementation: {}", impl.getClass().getSimpleName());
			return impl;
		}

		Path getCacheDir() {
			return cacheDir;
		}

		Path getConfigDir() {
			return configDir;
		}
	}

	private JadxCommonFiles() {
	}
}
