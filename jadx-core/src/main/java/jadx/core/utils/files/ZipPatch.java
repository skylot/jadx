package jadx.core.utils.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipPatch {
	private static final Logger LOG = LoggerFactory.getLogger(ZipPatch.class);

	public static List<File> patchZipFiles(List<File> inputs) {
		List<File> result = new ArrayList<>(inputs.size());
		for (File input : inputs) {
			try {
				result.add(patchZipFile(input));
			} catch (Throwable e) {
				LOG.warn("Failed to patch zip file: {}", input.getAbsolutePath(), e);
				result.add(input);
			}
		}
		return result;
	}

	private static File patchZipFile(File file) throws IOException {
		String fileName = file.getPath().toLowerCase();
		if (!fileName.endsWith(".apk") && !fileName.endsWith(".zip")) {
			return file;
		}

		var cDirEntriesToFix = new ArrayList<Long>();
		var localHeaders = new ArrayList<Long>();
		List<Long> localHeaderToFix;

		try (var raFile = new RandomAccessFile(file, "r")) {
			var endOfCDirOffset = findEndOfCentralDir(raFile);

			raFile.seek(endOfCDirOffset + 0x10);
			var cDirOffset = Integer.toUnsignedLong(Integer.reverseBytes(raFile.readInt()));
			raFile.seek(endOfCDirOffset + 0x0a);
			var cDirNumEntries = Short.toUnsignedLong(Short.reverseBytes(raFile.readShort()));

			for (long i = 0, off = cDirOffset; i < cDirNumEntries; i++) {
				var info = readHeader(raFile, off);

				if (!info.validCompression()) {
					cDirEntriesToFix.add(off);
				}

				raFile.seek(off + 0x2a);
				localHeaders.add(Integer.toUnsignedLong(Integer.reverseBytes(raFile.readInt())));

				off += info.dataOffset;
			}

			localHeaderToFix = localHeaders
					.stream()
					.filter(off -> !readHeaderVexxed(raFile, off).validCompression())
					.collect(Collectors.toList());

			if (cDirEntriesToFix.isEmpty() && localHeaderToFix.isEmpty()) {
				return file;
			}
		}

		var newFile = copyFile(file);

		try (var newRaFile = new RandomAccessFile(newFile, "rwd")) {

			for (var off : cDirEntriesToFix) {
				var info = readHeader(newRaFile, off);

				newRaFile.seek(off + 0x0a);
				newRaFile.writeShort(0);

				newRaFile.seek(off + 0x14);
				newRaFile.writeInt(Integer.reverseBytes((int) info.uncompressedSize));

			}

			for (var off : localHeaderToFix) {
				var info = readHeader(newRaFile, off);

				newRaFile.seek(off + 0x08);
				newRaFile.writeShort(0);

				newRaFile.seek(off + 0x12);
				newRaFile.writeInt(Integer.reverseBytes((int) info.uncompressedSize));

				newRaFile.seek(off + 0x1c);
				newRaFile.writeShort(0);

				moveBlockBack(newRaFile, off + info.dataOffset, info.uncompressedSize, info.extraLen);
			}
		}
		LOG.info("Input zip file patched: {}", file.getAbsolutePath());
		return newFile;
	}

	private static void moveBlockBack(RandomAccessFile file, long offset, long size, long delta) throws IOException {
		var buffer = new byte[1024 * 1024];

		while (size > 0) {
			var len = (int) Math.min(buffer.length, size);

			file.seek(offset);
			file.read(buffer, 0, len);
			file.seek(offset - delta);
			file.write(buffer, 0, len);

			size -= len;
			offset += len;
		}
	}

	private static File copyFile(File file) throws IOException {
		var newFile = FileUtils.createTempFile(file.getName()).toFile();
		try (var in = new FileInputStream(file)) {
			try (var out = new FileOutputStream(newFile)) {
				in.transferTo(out);
			}
		}
		return newFile;
	}

	private static long findEndOfCentralDir(RandomAccessFile file) throws IOException {
		var offset = file.length() - 0x15L + 1;

		do {
			if (offset <= 0) {
				throw new IllegalArgumentException("File is not a valid ZIP: End of central directory record not found");
			}
			file.seek(--offset);
		} while (Integer.reverseBytes(file.readInt()) != 0x06054b50);

		return offset;
	}

	private static class HeaderInfo {
		short compression;
		long uncompressedSize;
		long dataOffset;
		long extraLen;

		boolean validCompression() {
			return compression == 0x0 || compression == 0x8;
		}
	}

	private static HeaderInfo readHeaderVexxed(RandomAccessFile file, long offset) {
		try {
			return readHeader(file, offset);
		} catch (IOException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	private static HeaderInfo readHeader(RandomAccessFile file, long offset) throws IOException {
		var info = new HeaderInfo();

		file.seek(offset);
		var signature = Integer.reverseBytes(file.readInt());

		if (signature != 0x02014b50 && signature != 0x04034b50) {
			throw new IllegalArgumentException(
					String.format("Invalid ZIP header signature %x at offset %x",
							signature, offset));
		}

		var isCentralHeader = signature == 0x02014b50;
		var delta = isCentralHeader ? 0 : -2;

		file.seek(offset + 0x0a + delta);
		info.compression = Short.reverseBytes(file.readShort());

		file.seek(offset + 0x18 + delta);
		info.uncompressedSize = Integer.toUnsignedLong(Integer.reverseBytes(file.readInt()));

		file.seek(offset + 0x1c + delta);
		var nameLen = Short.toUnsignedLong(Short.reverseBytes(file.readShort()));
		info.extraLen = Short.toUnsignedLong(Short.reverseBytes(file.readShort()));
		var commentLen = 0L;

		if (isCentralHeader) {
			commentLen = Short.toUnsignedLong(Short.reverseBytes(file.readShort()));
		}

		info.dataOffset = (isCentralHeader ? 0x2e : 0x1e) + nameLen + info.extraLen + commentLen;

		return info;
	}
}
