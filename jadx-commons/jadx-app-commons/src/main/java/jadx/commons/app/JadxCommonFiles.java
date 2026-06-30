package jadx.commons.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dirs.ProjectDirectories;
import dev.dirs.impl.Windows;
import dev.dirs.impl.WindowsPowerShell;
import dev.dirs.jni.WindowsJni;

public class JadxCommonFiles {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCommonFiles.class);

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
				AtomicReference<@Nullable ProjectDirectories> pdRef = new AtomicReference<>();
				configDir = loadEnvDir("JADX_CONFIG_DIR", () -> loadDirs(pdRef).configDir);
				cacheDir = loadEnvDir("JADX_CACHE_DIR", () -> loadDirs(pdRef).cacheDir);
			} catch (Exception e) {
				throw new RuntimeException("Failed to init common directories", e);
			}
		}

		private static Path loadEnvDir(String envVar, Supplier<String> dirFunc) throws IOException {
			String envDir = JadxCommonEnv.get(envVar, null);
			String dirStr;
			if (envDir != null) {
				dirStr = envDir;
			} else {
				dirStr = dirFunc.get();
			}
			Path path = Path.of(dirStr).toAbsolutePath();
			Files.createDirectories(path);
			return path;
		}

		private static ProjectDirectories loadDirs(AtomicReference<@Nullable ProjectDirectories> pdRef) {
			ProjectDirectories currentDirs = pdRef.get();
			if (currentDirs != null) {
				return currentDirs;
			}
			LOG.debug("Loading system dirs ...");
			long start = System.currentTimeMillis();

			ProjectDirectories loadedDirs = ProjectDirectories.from("io.github", "skylot", "jadx", DirsLoader::getWinDirs);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Loaded system dirs ({}ms): config: {}, cache: {}",
						System.currentTimeMillis() - start, loadedDirs.configDir, loadedDirs.cacheDir);
			}
			pdRef.set(loadedDirs);
			return loadedDirs;
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
}
