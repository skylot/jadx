package jadx.plugins.input.dex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.plugins.input.dex.sections.DexConsts;

public class DexFileLoader {
	private static final Logger LOG = LoggerFactory.getLogger(DexFileLoader.class);

	public static List<DexReader> collectDexFiles(List<Path> pathsList) {
		return pathsList.stream()
				.map(path -> loadDexFromPath(path, 0))
				.filter(list -> !list.isEmpty())
				.flatMap(Collection::stream)
				.peek(dr -> LOG.debug("Loading dex: {}", dr))
				.collect(Collectors.toList());
	}

	private static List<DexReader> loadDexFromPath(Path path, int depth) {
		try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
			byte[] magic = new byte[DexConsts.MAX_MAGIC_SIZE];
			if (inputStream.read(magic) != magic.length) {
				return Collections.emptyList();
			}
			if (isStartWithBytes(magic, DexConsts.DEX_FILE_MAGIC)) {
				return Collections.singletonList(new DexReader(path));
			}
			if (depth == 0 && isStartWithBytes(magic, DexConsts.ZIP_FILE_MAGIC)) {
				return collectDexFromZip(path, depth);
			}
		} catch (Exception e) {
			LOG.error("File open error: {}", path, e);
		}
		return Collections.emptyList();
	}

	private static List<DexReader> collectDexFromZip(Path path, int depth) throws IOException {
		List<DexReader> result = new ArrayList<>();
		FileSystem zip = FileSystems.newFileSystem(path, (ClassLoader) null);
		for (Path rootDir : zip.getRootDirectories()) {
			Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					// TODO: add zip security checks
					result.addAll(loadDexFromPath(file, depth + 1));
					return FileVisitResult.CONTINUE;
				}
			});
		}
		return result;
	}

	private static boolean isStartWithBytes(byte[] fileMagic, byte[] expectedBytes) {
		int len = expectedBytes.length;
		if (fileMagic.length < len) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (fileMagic[i] != expectedBytes[i]) {
				return false;
			}
		}
		return true;
	}
}
