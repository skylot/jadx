package jadx.gui.utils.codecache.disk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class DiskCodeCache implements ICodeCache {
	private static final Logger LOG = LoggerFactory.getLogger(DiskCodeCache.class);

	private static final int DATA_FORMAT_VERSION = 2;

	private final Path srcDir;
	private final Path metaDir;
	private final Path codeVersionFile;
	private final String codeVersion;
	private final CodeMetadataAdapter codeMetadataAdapter;
	private final ExecutorService writePool;
	private final Map<String, ICodeInfo> writeOps = new ConcurrentHashMap<>();

	public DiskCodeCache(RootNode root, Path baseDir) {
		srcDir = baseDir.resolve("sources");
		metaDir = baseDir.resolve("metadata");
		codeVersionFile = baseDir.resolve("code-version");
		JadxArgs args = root.getArgs();
		codeVersion = buildCodeVersion(args);
		writePool = Executors.newFixedThreadPool(args.getThreadsCount());
		codeMetadataAdapter = new CodeMetadataAdapter(root);
		checkCodeVersion();
	}

	private String buildCodeVersion(JadxArgs args) {
		return String.format("%d:%s", DATA_FORMAT_VERSION, args.makeCodeArgsHash());
	}

	private void checkCodeVersion() {
		if (!Files.exists(codeVersionFile)) {
			reset();
			return;
		}
		try {
			String currentCodeVer = readFileToString(codeVersionFile);
			if (!currentCodeVer.equals(codeVersion)) {
				reset();
			}
		} catch (Exception e) {
			LOG.warn("Failed to load code version file", e);
			reset();
		}
	}

	private void reset() {
		try {
			LOG.debug("Resetting disk code cache, base dir: {}", srcDir.getParent().toAbsolutePath());
			FileUtils.deleteDirIfExists(srcDir);
			FileUtils.deleteDirIfExists(metaDir);
			FileUtils.makeDirs(srcDir);
			FileUtils.makeDirs(metaDir);
			writeFile(codeVersionFile, codeVersion);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to reset code cache", e);
		}
	}

	/**
	 * Async writes backed by in-memory store
	 */
	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		writeOps.put(clsFullName, codeInfo);
		writePool.execute(() -> {
			try {
				LOG.debug("Saving class info to disk: {}", clsFullName);
				writeFile(getJavaFile(clsFullName), codeInfo.getCodeStr());
				codeMetadataAdapter.write(getMetadataFile(clsFullName), codeInfo);
				writeOps.remove(clsFullName);
			} catch (Exception e) {
				LOG.error("Failed to write code cache for " + clsFullName, e);
				remove(clsFullName);
			}
		});
	}

	@Override
	public ICodeInfo get(String clsFullName) {
		try {
			ICodeInfo wrtCodeInfo = writeOps.get(clsFullName);
			if (wrtCodeInfo != null) {
				return wrtCodeInfo;
			}
			Path javaFile = getJavaFile(clsFullName);
			if (!Files.exists(javaFile)) {
				return ICodeInfo.EMPTY;
			}
			LOG.debug("Loading class from disk: {}", clsFullName);
			String code = readFileToString(javaFile);
			return codeMetadataAdapter.readAndBuild(getMetadataFile(clsFullName), code);
		} catch (Exception e) {
			LOG.error("Failed to read code cache for {}", clsFullName, e);
			return ICodeInfo.EMPTY;
		}
	}

	@Override
	public void remove(String clsFullName) {
		try {
			LOG.debug("Remove class info from disk: {}", clsFullName);
			Files.deleteIfExists(getJavaFile(clsFullName));
			Files.deleteIfExists(getMetadataFile(clsFullName));
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to remove code cache for " + clsFullName, e);
		}
	}

	private static String readFileToString(Path textFile) throws IOException {
		return new String(Files.readAllBytes(textFile), StandardCharsets.UTF_8);
	}

	private void writeFile(Path file, String data) {
		try {
			FileUtils.makeDirsForFile(file);
			Files.write(file, data.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			LOG.error("Failed to write file: {}", file.toAbsolutePath(), e);
		}
	}

	private Path getJavaFile(String clsFullName) {
		return srcDir.resolve(clsFullName.replace('.', '/') + ".java");
	}

	private Path getMetadataFile(String clsFullName) {
		return metaDir.resolve(clsFullName.replace('.', '/') + ".jadxmd");
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void close() throws IOException {
		try {
			writePool.shutdown();
			writePool.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOG.error("Failed to finish file writes", e);
		}
	}
}
