package jadx.gui.utils.codecache.disk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import jadx.api.args.UserRenamesMappingsMode;
import jadx.core.Jadx;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class DiskCodeCache implements ICodeCache {
	private static final Logger LOG = LoggerFactory.getLogger(DiskCodeCache.class);

	private static final int DATA_FORMAT_VERSION = 13;

	private static final byte[] JADX_NAMES_MAP_HEADER = "jadxnm".getBytes(StandardCharsets.US_ASCII);

	private final Path srcDir;
	private final Path metaDir;
	private final Path codeVersionFile;
	private final Path namesMapFile;
	private final String codeVersion;
	private final CodeMetadataAdapter codeMetadataAdapter;
	private final ExecutorService writePool;
	private final Map<String, ICodeInfo> writeOps = new ConcurrentHashMap<>();
	private final Map<String, Integer> namesMap = new ConcurrentHashMap<>();
	private final Map<String, Integer> allClsIds;

	public DiskCodeCache(RootNode root, JadxProject project, JadxSettings settings) {
		Path baseDir = project.getCacheDir();
		srcDir = baseDir.resolve("sources");
		metaDir = baseDir.resolve("metadata");
		codeVersionFile = baseDir.resolve("code-version");
		namesMapFile = baseDir.resolve("names-map");
		JadxArgs args = root.getArgs();
		codeVersion = buildCodeVersion(args, project, settings);
		writePool = Executors.newFixedThreadPool(args.getThreadsCount());
		codeMetadataAdapter = new CodeMetadataAdapter(root);
		allClsIds = buildClassIdsMap(root.getClasses());
		if (checkCodeVersion()) {
			loadNamesMap();
		} else {
			reset();
		}
	}

	private boolean checkCodeVersion() {
		try {
			if (!Files.exists(codeVersionFile)) {
				return false;
			}
			String currentCodeVer = FileUtils.readFile(codeVersionFile);
			return currentCodeVer.equals(codeVersion);
		} catch (Exception e) {
			LOG.warn("Failed to load code version file", e);
			return false;
		}
	}

	public void reset() {
		try {
			long start = System.currentTimeMillis();
			LOG.info("Resetting disk code cache, base dir: {}", srcDir.getParent().toAbsolutePath());
			FileUtils.deleteDirIfExists(srcDir);
			FileUtils.deleteDirIfExists(metaDir);
			FileUtils.deleteFileIfExists(namesMapFile);
			FileUtils.makeDirs(srcDir);
			FileUtils.makeDirs(metaDir);
			FileUtils.writeFile(codeVersionFile, codeVersion);
			if (LOG.isDebugEnabled()) {
				LOG.info("Reset done in: {}ms", System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to reset code cache", e);
		} finally {
			namesMap.clear();
		}
	}

	/**
	 * Async writes backed by in-memory store
	 */
	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		writeOps.put(clsFullName, codeInfo);
		int clsId = getClsId(clsFullName);
		namesMap.put(clsFullName, clsId);
		writePool.execute(() -> {
			try {
				FileUtils.writeFile(getJavaFile(clsId), codeInfo.getCodeStr());
				codeMetadataAdapter.write(getMetadataFile(clsId), codeInfo.getCodeMetadata());
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
			int clsId = getClsId(clsFullName);
			Path javaFile = getJavaFile(clsId);
			if (!Files.exists(javaFile)) {
				return null;
			}
			return FileUtils.readFile(javaFile);
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
			int clsId = getClsId(clsFullName);
			Path javaFile = getJavaFile(clsId);
			if (!Files.exists(javaFile)) {
				return ICodeInfo.EMPTY;
			}
			String code = FileUtils.readFile(javaFile);
			return codeMetadataAdapter.readAndBuild(getMetadataFile(clsId), code);
		} catch (Exception e) {
			LOG.error("Failed to read code cache for {}", clsFullName, e);
			return ICodeInfo.EMPTY;
		}
	}

	@Override
	public boolean contains(String clsFullName) {
		return namesMap.containsKey(clsFullName);
	}

	@Override
	public void remove(String clsFullName) {
		try {
			LOG.debug("Removing class info from disk: {}", clsFullName);
			Integer clsId = namesMap.remove(clsFullName);
			if (clsId != null) {
				Files.deleteIfExists(getJavaFile(clsId));
				Files.deleteIfExists(getMetadataFile(clsId));
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to remove code cache for " + clsFullName, e);
		}
	}

	private String buildCodeVersion(JadxArgs args, JadxProject project, JadxSettings settings) {
		long mappingsLastModified = -1;
		if (settings.getUserRenamesMappingsMode() != UserRenamesMappingsMode.IGNORE
				&& project.getMappingsPath() != null
				&& project.getMappingsPath().toFile().exists()) {
			mappingsLastModified = project.getMappingsPath().toFile().lastModified();
		}

		return DATA_FORMAT_VERSION
				+ ":" + Jadx.getVersion()
				+ ":" + args.makeCodeArgsHash()
				+ ":" + buildInputsHash(args.getInputFiles())
				+ ":" + mappingsLastModified;
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

	private int getClsId(String clsFullName) {
		Integer id = allClsIds.get(clsFullName);
		if (id == null) {
			throw new JadxRuntimeException("Unknown class name: " + clsFullName);
		}
		return id;
	}

	private void saveNamesMap() {
		LOG.debug("Saving names map for disk cache...");
		try (OutputStream fileOutput = Files.newOutputStream(namesMapFile, WRITE, CREATE, TRUNCATE_EXISTING);
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fileOutput))) {
			out.write(JADX_NAMES_MAP_HEADER);
			out.writeInt(namesMap.size());
			for (Map.Entry<String, Integer> entry : namesMap.entrySet()) {
				out.writeUTF(entry.getKey());
				out.writeInt(entry.getValue());
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to save names map file", e);
		}
	}

	private void loadNamesMap() {
		if (!Files.exists(namesMapFile)) {
			reset();
			return;
		}
		namesMap.clear();
		try (InputStream fileInput = Files.newInputStream(namesMapFile);
				DataInputStream in = new DataInputStream(new BufferedInputStream(fileInput))) {
			in.skipBytes(JADX_NAMES_MAP_HEADER.length);
			int count = in.readInt();
			for (int i = 0; i < count; i++) {
				String clsName = in.readUTF();
				int clsId = in.readInt();
				namesMap.put(clsName, clsId);
				Integer prevId = allClsIds.get(clsName);
				if (prevId == null || prevId != clsId) {
					LOG.debug("Unexpected class id, got: {}, expect: {}", clsId, prevId);
					LOG.warn("Inconsistent disk cache, resetting...");
					reset();
					return;
				}
			}
			LOG.info("Found {} classes in disk cache, dir: {}", count, metaDir.getParent());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load names map file", e);
		}
	}

	private Path getJavaFile(int clsId) {
		return srcDir.resolve(getPathForClsId(clsId, ".java"));
	}

	private Path getMetadataFile(int clsId) {
		return metaDir.resolve(getPathForClsId(clsId, ".jadxmd"));
	}

	private Path getPathForClsId(int clsId, String ext) {
		// all classes divided between 256 top level folders
		String firstByte = FileUtils.byteToHex(clsId);
		return Paths.get(firstByte, FileUtils.intToHex(clsId) + ext);
	}

	private Map<String, Integer> buildClassIdsMap(List<ClassNode> classes) {
		int clsCount = classes.size();
		Map<String, Integer> map = new HashMap<>(clsCount);
		for (int i = 0; i < clsCount; i++) {
			ClassNode cls = classes.get(i);
			map.put(cls.getRawName(), i);
		}
		return map;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void close() throws IOException {
		try {
			saveNamesMap();
			writePool.shutdown();
			writePool.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOG.error("Failed to finish file writes", e);
		}
	}
}
