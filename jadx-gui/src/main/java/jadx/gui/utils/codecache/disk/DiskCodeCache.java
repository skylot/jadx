package jadx.gui.utils.codecache.disk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class DiskCodeCache implements ICodeCache {
	private static final Logger LOG = LoggerFactory.getLogger(DiskCodeCache.class);

	private static final int DATA_FORMAT_VERSION = 7;

	private final Path srcDir;
	private final Path metaDir;
	private final Path codeVersionFile;
	private final String codeVersion;
	private final CodeMetadataAdapter codeMetadataAdapter;
	private final ExecutorService writePool;
	private final Map<String, ICodeInfo> writeOps = new ConcurrentHashMap<>();
	private final Set<String> cachedKeys = Collections.synchronizedSet(new HashSet<>());

	public DiskCodeCache(RootNode root, Path baseDir) {
		srcDir = baseDir.resolve("sources");
		metaDir = baseDir.resolve("metadata");
		codeVersionFile = baseDir.resolve("code-version");
		JadxArgs args = root.getArgs();
		codeVersion = buildCodeVersion(args);
		writePool = Executors.newFixedThreadPool(args.getThreadsCount());
		codeMetadataAdapter = new CodeMetadataAdapter(root);
		if (checkCodeVersion()) {
			collectCachedItems();
		} else {
			reset();
		}
	}

	private boolean checkCodeVersion() {
		try {
			if (!Files.exists(codeVersionFile)) {
				return false;
			}
			String currentCodeVer = readFileToString(codeVersionFile);
			return currentCodeVer.equals(codeVersion);
		} catch (Exception e) {
			LOG.warn("Failed to load code version file", e);
			return false;
		}
	}

	private void reset() {
		try {
			long start = System.currentTimeMillis();
			LOG.info("Resetting disk code cache, base dir: {}", srcDir.getParent().toAbsolutePath());
			FileUtils.deleteDirIfExists(srcDir);
			FileUtils.deleteDirIfExists(metaDir);
			FileUtils.makeDirs(srcDir);
			FileUtils.makeDirs(metaDir);
			writeFile(codeVersionFile, codeVersion);
			cachedKeys.clear();
			if (LOG.isDebugEnabled()) {
				LOG.info("Reset done in: {}ms", System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to reset code cache", e);
		}
	}

	private void collectCachedItems() {
		cachedKeys.clear();
		try {
			long start = System.currentTimeMillis();
			PathMatcher matcher = metaDir.getFileSystem().getPathMatcher("glob:**.jadxmd");
			Files.walkFileTree(metaDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (matcher.matches(file)) {
						Path relPath = metaDir.relativize(file);
						String filePath = relPath.toString();
						String clsName = filePath.substring(0, filePath.length() - 7).replace(File.separatorChar, '.');
						cachedKeys.add(clsName);
					}
					return FileVisitResult.CONTINUE;
				}
			});
			LOG.info("Found {} classes in disk cache in {} ms", cachedKeys.size(), System.currentTimeMillis() - start);
		} catch (Exception e) {
			LOG.error("Failed to collect cached items", e);
		}
	}

	/**
	 * Async writes backed by in-memory store
	 */
	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		writeOps.put(clsFullName, codeInfo);
		cachedKeys.add(clsFullName);
		writePool.execute(() -> {
			try {
				writeFile(getJavaFile(clsFullName), codeInfo.getCodeStr());
				codeMetadataAdapter.write(getMetadataFile(clsFullName), codeInfo.getCodeMetadata());
			} catch (Exception e) {
				LOG.error("Failed to write code cache for " + clsFullName, e);
				remove(clsFullName);
			} finally {
				writeOps.remove(clsFullName);
			}
		});
	}

	@Override
	public @Nullable String getCode(String clsFullName) {
		try {
			if (!contains(clsFullName)) {
				return null;
			}
			ICodeInfo wrtCodeInfo = writeOps.get(clsFullName);
			if (wrtCodeInfo != null) {
				return wrtCodeInfo.getCodeStr();
			}
			Path javaFile = getJavaFile(clsFullName);
			if (!Files.exists(javaFile)) {
				return null;
			}
			return readFileToString(javaFile);
		} catch (Exception e) {
			LOG.error("Failed to read class code for {}", clsFullName, e);
			return null;
		}
	}

	@Override
	public ICodeInfo get(String clsFullName) {
		try {
			if (!contains(clsFullName)) {
				return ICodeInfo.EMPTY;
			}
			ICodeInfo wrtCodeInfo = writeOps.get(clsFullName);
			if (wrtCodeInfo != null) {
				return wrtCodeInfo;
			}
			Path javaFile = getJavaFile(clsFullName);
			if (!Files.exists(javaFile)) {
				return ICodeInfo.EMPTY;
			}
			String code = readFileToString(javaFile);
			return codeMetadataAdapter.readAndBuild(getMetadataFile(clsFullName), code);
		} catch (Exception e) {
			LOG.error("Failed to read code cache for {}", clsFullName, e);
			return ICodeInfo.EMPTY;
		}
	}

	@Override
	public boolean contains(String clsFullName) {
		return cachedKeys.contains(clsFullName);
	}

	@Override
	public void remove(String clsFullName) {
		try {
			LOG.debug("Removing class info from disk: {}", clsFullName);
			cachedKeys.remove(clsFullName);
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

	private String buildCodeVersion(JadxArgs args) {
		return DATA_FORMAT_VERSION
				+ ":" + args.makeCodeArgsHash()
				+ ":" + buildInputsHash(args.getInputFiles());
	}

	/**
	 * Hash timestamps of all input files
	 */
	private String buildInputsHash(List<File> inputs) {
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream data = new DataOutputStream(bout)) {
			List<Path> inputPaths = Utils.collectionMap(inputs, File::toPath);
			List<Path> inputFiles = FileUtils.expandDirs(inputPaths);
			Collections.sort(inputFiles);
			data.write(inputs.size());
			data.write(inputFiles.size());
			for (Path inputFile : inputFiles) {
				FileTime modifiedTime = Files.getLastModifiedTime(inputFile);
				data.writeLong(modifiedTime.toMillis());
			}
			return FileUtils.md5Sum(bout.toByteArray());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to build hash for inputs", e);
		}
	}

	private Path getJavaFile(String clsFullName) {
		return srcDir.resolve(clsFullName.replace('.', File.separatorChar) + ".java");
	}

	private Path getMetadataFile(String clsFullName) {
		return metaDir.resolve(clsFullName.replace('.', File.separatorChar) + ".jadxmd");
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
