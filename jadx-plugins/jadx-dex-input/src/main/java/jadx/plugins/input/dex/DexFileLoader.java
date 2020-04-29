package jadx.plugins.input.dex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
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
				.map((Path path) -> loadDexFromPath(path, 0))
				.flatMap(Collection::stream)
				.peek(dr -> LOG.debug("Loading dex: {}", dr))
				.collect(Collectors.toList());
	}

	private static List<DexReader> loadDexFromPath(Path path, int depth) {
		try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
			if (isDex(fileChannel)) {
				return Collections.singletonList(new DexReader(path, fileChannel));
			}
			if (depth == 0 && isZip(fileChannel)) {
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

	private static boolean isDex(FileChannel fileChannel) {
		return isStartWithBytes(fileChannel, DexConsts.DEX_FILE_MAGIC);
	}

	private static boolean isZip(FileChannel fileChannel) {
		return isStartWithBytes(fileChannel, DexConsts.ZIP_FILE_MAGIC);
	}

	private static boolean isStartWithBytes(FileChannel fileChannel, byte[] startBytes) {
		try {
			fileChannel.position(0);
			ByteBuffer buf = ByteBuffer.allocate(startBytes.length);
			fileChannel.read(buf);
			return Arrays.equals(startBytes, buf.array());
		} catch (Exception e) {
			return false;
		}
	}
}
