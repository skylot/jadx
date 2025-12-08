package jadx.plugins.input.xapk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPluginContext;
import jadx.core.utils.GsonUtils;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.input.xapk.data.SplitApk;
import jadx.plugins.input.xapk.data.XApkData;
import jadx.plugins.input.xapk.data.XApkManifest;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;

public class XApkLoader {
	private static final Logger LOG = LoggerFactory.getLogger(XApkLoader.class);

	private final JadxPluginContext context;
	private final Map<String, XApkData> loaded = new HashMap<>();

	public XApkLoader(JadxPluginContext context) {
		this.context = context;
	}

	public @Nullable XApkData checkAndLoad(Path inputPath) {
		String fileName = inputPath.getFileName().toString();
		if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xapk")) {
			return null;
		}
		try {
			XApkData loadedData = getLoaded(inputPath);
			if (loadedData != null) {
				return loadedData;
			}
			File xapkFile = inputPath.toFile();
			if (!FileUtils.isZipFile(xapkFile)) {
				return null;
			}
			try (ZipContent content = context.getZipReader().open(xapkFile)) {
				IZipEntry manifestEntry = content.searchEntry("manifest.json");
				if (manifestEntry == null) {
					return null;
				}
				String manifestStr = new String(manifestEntry.getBytes(), StandardCharsets.UTF_8);
				XApkManifest xApkManifest = GsonUtils.buildGson().fromJson(manifestStr, XApkManifest.class);
				if (xApkManifest.getVersion() != 2 || xApkManifest.getSplitApks().isEmpty()) {
					return null;
				}
				// checks complete
				// unpack all files into temp directory
				XApkData xApkData = unpackXApk(xapkFile, xApkManifest, content);
				saveLoaded(inputPath, xApkData);
				return xApkData;
			}
		} catch (Exception e) {
			LOG.warn("Failed to load XApk file: {}", inputPath.toAbsolutePath(), e);
			return null;
		}
	}

	private XApkData unpackXApk(File xapkFile, XApkManifest xApkManifest, ZipContent content) throws IOException {
		Set<String> declaredApks = xApkManifest.getSplitApks().stream()
				.map(SplitApk::getFile).collect(Collectors.toSet());
		List<Path> apks = new ArrayList<>(declaredApks.size());
		List<Path> files = new ArrayList<>();

		String dirName = xapkFile.getName() + '_' + System.currentTimeMillis();
		Path tmpDir = context.files().getPluginTempDir().resolve(dirName);
		FileUtils.makeDirs(tmpDir);
		for (IZipEntry entry : content.getEntries()) {
			String fileName = entry.getName();
			Path file = tmpDir.resolve(fileName);
			try (InputStream inputStream = entry.getInputStream()) {
				Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
			}
			if (declaredApks.contains(fileName)) {
				apks.add(file);
			} else {
				files.add(file);
			}
		}
		return new XApkData(xApkManifest, tmpDir, apks, files);
	}

	private XApkData getLoaded(Path inputPath) throws IOException {
		return loaded.get(pathToKey(inputPath));
	}

	private void saveLoaded(Path inputPath, XApkData xApkData) throws IOException {
		loaded.put(pathToKey(inputPath), xApkData);
	}

	private static String pathToKey(Path path) throws IOException {
		return path.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
	}

	public synchronized void unload() {
		for (XApkData data : loaded.values()) {
			FileUtils.deleteDirIfExists(data.getTmpDir());
		}
		loaded.clear();
	}
}
