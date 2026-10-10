package jadx.zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for forged zip local file header sizes on STORED entries.
 * <p>
 * The custom parser must not trust declared entry sizes: a STORED entry is stored
 * uncompressed, so its compressed and uncompressed sizes are always equal and its
 * data physically ends inside the archive. A forged header can otherwise drive an
 * allocation past the real data (buffer underflow / oversized allocation) instead of
 * falling back to the JDK parser, which reads from the central directory.
 * <p>
 * Follow-up to the forged-header zip bomb hardening from issues #980 / PR #982.
 */
class ZipForgedHeaderTest {

	@TempDir
	Path tmp;

	private static final String ENTRY_NAME = "classes.dex";
	private static final byte[] DATA = "hello-jadx-stored-entry".getBytes(StandardCharsets.UTF_8);

	// Declared uncompressed size below the 25 MB zip-bomb exemption, large enough to
	// prove the parser bounds the read by the real archive instead of the header value.
	private static final int FORGED_USIZE = 5_000_000;
	// Forged compressed size kept small enough to stay inside the tiny archive during
	// enumeration, yet different from the uncompressed size (impossible for STORED).
	private static final int FORGED_CSIZE = 80;

	private static byte[] buildStoredZip() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(bos)) {
			ZipEntry entry = new ZipEntry(ENTRY_NAME);
			entry.setMethod(ZipEntry.STORED);
			CRC32 crc = new CRC32();
			crc.update(DATA);
			entry.setSize(DATA.length);
			entry.setCompressedSize(DATA.length);
			entry.setCrc(crc.getValue());
			zos.putNextEntry(entry);
			zos.write(DATA);
			zos.closeEntry();
		}
		return bos.toByteArray();
	}

	private static int indexOf(byte[] haystack, byte[] needle) {
		for (int i = 0; i <= haystack.length - needle.length; i++) {
			boolean match = true;
			for (int j = 0; j < needle.length; j++) {
				if (haystack[i + j] != needle[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return i;
			}
		}
		return -1;
	}

	private static void putLittleEndianInt(byte[] out, int offset, int value) {
		out[offset] = (byte) (value & 0xff);
		out[offset + 1] = (byte) ((value >> 8) & 0xff);
		out[offset + 2] = (byte) ((value >> 16) & 0xff);
		out[offset + 3] = (byte) ((value >> 24) & 0xff);
	}

	/** Overwrite the local file header compressed (+18) and uncompressed (+22) sizes. */
	private static byte[] withLocalHeaderSizes(byte[] zip, int compressedSize, int uncompressedSize) {
		int lfh = indexOf(zip, new byte[] { 0x50, 0x4b, 0x03, 0x04 });
		assertThat(lfh).isGreaterThanOrEqualTo(0);
		byte[] result = zip.clone();
		putLittleEndianInt(result, lfh + 18, compressedSize);
		putLittleEndianInt(result, lfh + 22, uncompressedSize);
		return result;
	}

	private File toApk(byte[] zip, String name) throws IOException {
		File file = tmp.resolve(name).toFile();
		Files.write(file.toPath(), zip);
		return file;
	}

	private static byte[] readFirstEntryBytes(File file) throws IOException {
		try (ZipContent content = new ZipReader().open(file)) {
			return content.getEntries().get(0).getBytes();
		}
	}

	private static byte[] readFirstEntryStream(File file) throws IOException {
		byte[][] holder = new byte[1][];
		new ZipReader().readEntries(file, (entry, in) -> {
			try {
				holder[0] = in.readAllBytes();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		return holder[0];
	}

	@Test
	void readsValidStoredEntry() throws IOException {
		byte[] content = readFirstEntryBytes(toApk(buildStoredZip(), "valid.apk"));
		assertThat(content).isEqualTo(DATA);
	}

	@Test
	void forgedInconsistentStoredSizesFallBackToRealContent() throws IOException {
		byte[] zip = withLocalHeaderSizes(buildStoredZip(), FORGED_CSIZE, FORGED_USIZE);
		// Apk extension forces the custom parser; it must recover via the fallback parser.
		byte[] content = readFirstEntryBytes(toApk(zip, "forged-inconsistent.apk"));
		assertThat(content).isEqualTo(DATA);
	}

	@Test
	void forgedInconsistentStoredSizesFallBackWhenStreaming() throws IOException {
		byte[] zip = withLocalHeaderSizes(buildStoredZip(), FORGED_CSIZE, FORGED_USIZE);
		byte[] content = readFirstEntryStream(toApk(zip, "forged-stream.apk"));
		assertThat(content).isEqualTo(DATA);
	}

	@Test
	void forgedOversizedStoredSizesFallBackToRealContent() throws IOException {
		byte[] zip = withLocalHeaderSizes(buildStoredZip(), FORGED_USIZE, FORGED_USIZE);
		byte[] content = readFirstEntryBytes(toApk(zip, "forged-oversized.apk"));
		assertThat(content).isEqualTo(DATA);
	}
}
