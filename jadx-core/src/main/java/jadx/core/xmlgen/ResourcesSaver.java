package jadx.core.xmlgen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.api.security.IJadxSecurity;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class ResourcesSaver implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesSaver.class);

	private final ResourceFile resourceFile;
	private final Path outDir;
	private final IJadxSecurity security;

	public ResourcesSaver(JadxDecompiler decompiler, File outDir, ResourceFile resourceFile) {
		this.resourceFile = resourceFile;
		this.outDir = outDir.toPath();
		this.security = decompiler.getArgs().getSecurity();
	}

	@Override
	public void run() {
		try {
			saveResources(resourceFile.loadContent());
		} catch (StackOverflowError | Exception e) {
			LOG.warn("Failed to save resource: {}", resourceFile.getOriginalName(), e);
		}
	}

	private void saveResources(ResContainer rc) {
		if (rc == null) {
			return;
		}
		if (rc.getDataType() == ResContainer.DataType.RES_TABLE) {
			saveToFile(rc, outDir.resolve("res/values/public.xml"));
			for (ResContainer subFile : rc.getSubFiles()) {
				saveResources(subFile);
			}
		} else {
			save(rc, outDir);
		}
	}

	private void save(ResContainer rc, Path baseDir) {
		Path outFile = baseDir.resolve(rc.getFileName());
		if (!security.isInSubDirectory(baseDir.toFile(), outFile.toFile())) {
			LOG.error("Invalid resource name or path traversal attack detected: {}", outFile);
			return;
		}
		saveToFile(rc, outFile);
	}

	private void saveToFile(ResContainer rc, Path outFile) {
		Path targetFile = resolveNameConflict(outFile);
		switch (rc.getDataType()) {
			case TEXT:
			case RES_TABLE:
				SaveCode.save(rc.getText(), targetFile.toFile());
				return;

			case DECODED_DATA:
				byte[] data = rc.getDecodedData();
				FileUtils.makeDirsForFile(targetFile);
				try {
					Files.write(targetFile, data);
				} catch (Exception e) {
					LOG.warn("Resource '{}' not saved, got exception", rc.getName(), e);
				}
				return;

			case RES_LINK:
				ResourceFile resFile = rc.getResLink();
				FileUtils.makeDirsForFile(targetFile);
				try {
					saveResourceFile(resFile, targetFile);
				} catch (Exception e) {
					LOG.warn("Resource '{}' not saved, got exception", rc.getName(), e);
				}
				return;

			default:
				LOG.warn("Resource '{}' not saved, unknown type", rc.getName());
				break;
		}
	}

	private void saveResourceFile(ResourceFile resFile, Path outFile) throws JadxException {
		ResourcesLoader.decodeStream(resFile, (size, is) -> {
			try {
				Files.copy(is, outFile, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				Files.deleteIfExists(outFile); // delete partially written file
				throw new JadxRuntimeException("Resource file save error", e);
			}
			return null;
		});
	}

	/**
	 * Pick a file name which doesn't collide with an already saved file.
	 * <p>
	 * Zip entry names are case sensitive, but file systems are usually not: an entry named
	 * 'ANDROIDMANIFEST.XML' placed after 'AndroidManifest.xml' will overwrite the manifest
	 * on Windows and macOS. Such entries are saved under a different name.
	 */
	private static Path resolveNameConflict(Path outFile) {
		if (!hasNameConflict(outFile)) {
			return outFile;
		}
		String name = outFile.getFileName().toString();
		int dotPos = name.lastIndexOf('.');
		String baseName = dotPos > 0 ? name.substring(0, dotPos) : name;
		String extension = dotPos > 0 ? name.substring(dotPos) : "";
		for (int i = 1; i < 100; i++) {
			Path candidate = outFile.resolveSibling(baseName + '_' + i + extension);
			if (!hasNameConflict(candidate)) {
				LOG.warn("Resource '{}' conflicts with already saved file (case insensitive), saved as: {}",
						name, candidate.getFileName());
				return candidate;
			}
		}
		LOG.warn("Can't resolve resource name conflict for: {}", outFile);
		return outFile;
	}

	/**
	 * Check if a file with the same name (ignoring case) is already saved.
	 * <p>
	 * Only the file system is asked, no OS specific checks are needed: on a case insensitive
	 * file system the real name of an existing file differs from the requested one, while on a
	 * case sensitive file system (including case sensitive directories on Windows) a file with
	 * the same name in another case doesn't exist at all.
	 */
	private static boolean hasNameConflict(Path outFile) {
		try {
			if (!Files.exists(outFile)) {
				return false;
			}
			// compare names as strings: Path.equals is case insensitive on Windows,
			// so it can't be used to detect a name which differs only in case
			String actualName = outFile.toRealPath().getFileName().toString();
			return !actualName.equals(outFile.getFileName().toString());
		} catch (IOException e) {
			LOG.debug("Can't get real path for: {}", outFile, e);
			return false;
		}
	}
}
