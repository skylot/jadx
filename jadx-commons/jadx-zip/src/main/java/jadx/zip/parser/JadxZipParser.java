package jadx.zip.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.zip.IZipEntry;
import jadx.zip.IZipParser;
import jadx.zip.ZipContent;
import jadx.zip.ZipReaderFlags;
import jadx.zip.ZipReaderOptions;
import jadx.zip.fallback.FallbackZipParser;
import jadx.zip.io.ByteBufferBackedInputStream;
import jadx.zip.io.LimitedInputStream;
import jadx.zip.security.IJadxZipSecurity;

/**
 * Custom and simple zip parser to fight tampering.
 * Many zip features aren't supported:
 * - Compression methods other than STORE or DEFLATE
 * - Zip64
 * - Checksum verification
 * - Multi file archives
 */
public final class JadxZipParser implements IZipParser {
	private static final Logger LOG = LoggerFactory.getLogger(JadxZipParser.class);

	private static final byte LOCAL_FILE_HEADER_START = 0x50;
	private static final int LOCAL_FILE_HEADER_SIGN = 0x04034b50;
	private static final int CD_SIGN = 0x02014b50;
	private static final int END_OF_CD_SIGN = 0x06054b50;

	private final File zipFile;
	private final ZipReaderOptions options;
	private final IJadxZipSecurity zipSecurity;
	private final Set<ZipReaderFlags> flags;
	private final boolean verify;
	private final boolean useLimitedDataStream;

	private RandomAccessFile file;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;

	private int endOfCDStart = -2;

	private @Nullable ZipContent fallbackZipContent;

	public JadxZipParser(File zipFile, ZipReaderOptions options) {
		this.zipFile = zipFile;
		this.options = options;
		this.zipSecurity = options.getZipSecurity();
		this.flags = options.getFlags();
		this.verify = options.getFlags().contains(ZipReaderFlags.REPORT_TAMPERING);
		this.useLimitedDataStream = zipSecurity.useLimitedDataStream();
	}

	@Override
	public ZipContent open() throws IOException {
		load();
		try {
			int maxEntriesCount = zipSecurity.getMaxEntriesCount();
			if (maxEntriesCount == -1) {
				maxEntriesCount = Integer.MAX_VALUE;
			}
			List<IZipEntry> entries;
			if (flags.contains(ZipReaderFlags.IGNORE_CENTRAL_DIR_ENTRIES)) {
				entries = searchLocalFileHeaders(maxEntriesCount);
			} else {
				entries = loadFromCentralDirs(maxEntriesCount);
			}
			return new ZipContent(this, entries);
		} catch (Exception e) {
			if (flags.contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
				throw new IOException("Failed to open zip: " + zipFile + ", error: " + e.getMessage(), e);
			}
			LOG.warn("Zip open failed, switching to fallback parser, zip: {}", zipFile, e);
			return initFallbackParser();
		}
	}

	@SuppressWarnings("RedundantIfStatement")
	public boolean canOpen() {
		try {
			load();
			int eocdStart = searchEndOfCDStart();
			ByteBuffer buf = byteBuffer;
			buf.position(eocdStart + 4);
			int diskNum = readU2(buf);
			if (diskNum == 0xFFFF) {
				// Zip64
				return false;
			}
			return true;
		} catch (Exception e) {
			LOG.warn("Jadx parser can't open zip file: {}", zipFile, e);
			return false;
		}
	}

	private boolean isValidEntry(JadxZipEntry zipEntry) {
		boolean validEntry = zipSecurity.isValidEntry(zipEntry);
		if (!validEntry) {
			LOG.warn("Zip entry '{}' is invalid and excluded from processing", zipEntry);
		}
		return validEntry;
	}

	private void load() throws IOException {
		if (byteBuffer != null) {
			// already loaded
			return;
		}
		file = new RandomAccessFile(zipFile, "r");
		long size = file.length();
		if (size >= Integer.MAX_VALUE) {
			throw new IOException("Zip file is too big");
		}
		int fileLen = (int) size;
		if (fileLen < 100 * 1024 * 1024) {
			// load files smaller than 100MB directly into memory
			byte[] bytes = new byte[fileLen];
			file.readFully(bytes);
			byteBuffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
			file.close();
			file = null;
		} else {
			// for big files - use a memory mapped file
			fileChannel = file.getChannel();
			byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		}
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	private List<IZipEntry> searchLocalFileHeaders(int maxEntriesCount) {
		List<IZipEntry> entries = new ArrayList<>();
		while (true) {
			int start = searchEntryStart();
			if (start == -1) {
				return entries;
			}
			JadxZipEntry zipEntry = loadFileEntry(start);
			if (isValidEntry(zipEntry)) {
				entries.add(zipEntry);
				if (entries.size() > maxEntriesCount) {
					throw new IllegalStateException("Max entries count limit exceeded: " + entries.size());
				}
			}
		}
	}

	private List<IZipEntry> loadFromCentralDirs(int maxEntriesCount) throws IOException {
		int eocdStart = searchEndOfCDStart();
		if (eocdStart < 0) {
			throw new RuntimeException("End of central directory not found");
		}
		ByteBuffer buf = byteBuffer;
		buf.position(eocdStart + 10);
		int entriesCount = readU2(buf);
		buf.position(eocdStart + 16);
		int cdOffset = buf.getInt();

		if (entriesCount > maxEntriesCount) {
			throw new IllegalStateException("Max entries count limit exceeded: " + entriesCount);
		}
		List<IZipEntry> entries = new ArrayList<>(entriesCount);
		buf.position(cdOffset);
		for (int i = 0; i < entriesCount; i++) {
			JadxZipEntry zipEntry = loadCDEntry();
			if (isValidEntry(zipEntry)) {
				entries.add(zipEntry);
			}
		}
		return entries;
	}

	private JadxZipEntry loadCDEntry() {
		ByteBuffer buf = byteBuffer;
		int start = buf.position();
		buf.position(start + 28);
		int fileNameLen = readU2(buf);
		int extraFieldLen = readU2(buf);
		int commentLen = readU2(buf);
		buf.position(start + 42);
		int fileEntryStart = buf.getInt();
		int entryEnd = start + 46 + fileNameLen + extraFieldLen + commentLen;
		JadxZipEntry entry = loadFileEntry(fileEntryStart);
		if (verify) {
			compareCDAndLFH(buf, start, entry);
		}
		if (!entry.isSizesValid()) {
			entry = fixEntryFromCD(entry, start);
		}
		buf.position(entryEnd);
		return entry;
	}

	private JadxZipEntry fixEntryFromCD(JadxZipEntry entry, int start) {
		ByteBuffer buf = byteBuffer;
		buf.position(start + 10);
		int comprMethod = readU2(buf);
		buf.position(start + 20);
		int comprSize = buf.getInt();
		int unComprSize = buf.getInt();
		return new JadxZipEntry(this, entry.getName(), start, entry.getDataStart(), comprMethod, comprSize, unComprSize);
	}

	private static void compareCDAndLFH(ByteBuffer buf, int start, JadxZipEntry entry) {
		buf.position(start + 10);
		int comprMethod = readU2(buf);
		if (comprMethod != entry.getCompressMethod()) {
			LOG.warn("Compression method differ in CD {} and LFH {} for {}",
					comprMethod, entry.getCompressMethod(), entry);
		}
		buf.position(start + 20);
		int comprSize = buf.getInt();
		int unComprSize = buf.getInt();
		if (comprSize != entry.getCompressedSize()) {
			LOG.warn("Compressed size differ in CD {} and LFH {} for {}",
					comprSize, entry.getCompressedSize(), entry);
		}
		if (unComprSize != entry.getUncompressedSize()) {
			LOG.warn("Uncompressed size differ in CD {} and LFH {} for {}",
					unComprSize, entry.getUncompressedSize(), entry);
		}
	}

	private JadxZipEntry loadFileEntry(int start) {
		ByteBuffer buf = byteBuffer;
		buf.position(start + 8);
		int comprMethod = readU2(buf);
		buf.position(start + 18);
		int comprSize = buf.getInt();
		int unComprSize = buf.getInt();
		int fileNameLen = readU2(buf);
		int extraFieldLen = readU2(buf);
		String fileName = readString(buf, fileNameLen);
		int dataStart = start + 30 + fileNameLen + extraFieldLen;
		buf.position(dataStart + comprSize);
		return new JadxZipEntry(this, fileName, start, dataStart, comprMethod, comprSize, unComprSize);
	}

	private int searchEndOfCDStart() throws IOException {
		if (endOfCDStart != -2) {
			return endOfCDStart;
		}
		ByteBuffer buf = byteBuffer;
		int pos = buf.limit() - 22;
		int minPos = Math.max(0, pos - 0xffff);
		while (true) {
			buf.position(pos);
			int sign = buf.getInt();
			if (sign == END_OF_CD_SIGN) {
				endOfCDStart = pos;
				return pos;
			}
			pos--;
			if (pos < minPos) {
				throw new IOException("End of central directory record not found");
			}
		}
	}

	private int searchEntryStart() {
		ByteBuffer buf = byteBuffer;
		while (true) {
			int start = buf.position();
			if (start + 4 > buf.limit()) {
				return -1;
			}
			byte b = buf.get();
			if (b == LOCAL_FILE_HEADER_START) {
				buf.position(start);
				int sign = buf.getInt();
				if (sign == LOCAL_FILE_HEADER_SIGN) {
					return start;
				}
			}
		}
	}

	synchronized InputStream getInputStream(JadxZipEntry entry) {
		if (verify) {
			verifyEntry(entry);
		}
		InputStream stream;
		if (entry.getCompressMethod() == 8) {
			try {
				stream = ZipDeflate.decompressEntryToStream(byteBuffer, entry);
			} catch (Exception e) {
				entryParseFailed(entry, e);
				return useFallbackParser(entry).getInputStream();
			}
		} else {
			// treat any other compression methods values as UNCOMPRESSED
			stream = bufferToStream(byteBuffer, entry.getDataStart(), (int) entry.getUncompressedSize());
		}
		if (useLimitedDataStream) {
			return new LimitedInputStream(stream, entry.getUncompressedSize());
		}
		return stream;
	}

	synchronized byte[] getBytes(JadxZipEntry entry) {
		if (verify) {
			verifyEntry(entry);
		}
		if (entry.getCompressMethod() == 8) {
			try {
				return ZipDeflate.decompressEntryToBytes(byteBuffer, entry);
			} catch (Exception e) {
				entryParseFailed(entry, e);
				return useFallbackParser(entry).getBytes();
			}
		}
		// treat any other compression methods values as UNCOMPRESSED
		return bufferToBytes(byteBuffer, entry.getDataStart(), (int) entry.getUncompressedSize());
	}

	private static void verifyEntry(JadxZipEntry entry) {
		int compressMethod = entry.getCompressMethod();
		if (compressMethod == 0) {
			if (entry.getCompressedSize() != entry.getUncompressedSize()) {
				LOG.warn("Not equal sizes for STORE method: compressed: {}, uncompressed: {}, entry: {}",
						entry.getCompressedSize(), entry.getUncompressedSize(), entry);
			}
		} else if (compressMethod != 8) {
			LOG.warn("Unknown compress method: {} in entry: {}", compressMethod, entry);
		}
	}

	private void entryParseFailed(JadxZipEntry entry, Exception e) {
		if (isEncrypted(entry)) {
			throw new RuntimeException("Entry is encrypted, failed to decompress: " + entry, e);
		}
		if (flags.contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
			throw new RuntimeException("Failed to decompress zip entry: " + entry + ", error: " + e.getMessage(), e);
		}
		LOG.warn("Entry '{}' parse failed, switching to fallback parser", entry, e);
	}

	@SuppressWarnings("resource")
	private IZipEntry useFallbackParser(JadxZipEntry entry) {
		LOG.debug("useFallbackParser used for {}", entry);
		IZipEntry zipEntry = initFallbackParser().searchEntry(entry.getName());
		if (zipEntry == null) {
			throw new RuntimeException("Fallback parser can't find entry: " + entry);
		}
		return zipEntry;
	}

	@SuppressWarnings("resource")
	private ZipContent initFallbackParser() {
		if (fallbackZipContent == null) {
			try {
				fallbackZipContent = new FallbackZipParser(zipFile, options).open();
			} catch (Exception e) {
				throw new RuntimeException("Fallback parser failed to open file: " + zipFile, e);
			}
		}
		return fallbackZipContent;
	}

	private boolean isEncrypted(JadxZipEntry entry) {
		int flags = readFlags(entry);
		return (flags & 1) != 0;
	}

	private int readFlags(JadxZipEntry entry) {
		ByteBuffer buf = byteBuffer;
		buf.position(entry.getEntryStart() + 6);
		return readU2(buf);
	}

	static byte[] bufferToBytes(ByteBuffer buf, int start, int size) {
		byte[] data = new byte[size];
		buf.position(start);
		buf.get(data);
		return data;
	}

	static InputStream bufferToStream(ByteBuffer buf, int start, int size) {
		buf.position(start);
		ByteBuffer streamBuf = buf.slice();
		streamBuf.limit(size);
		return new ByteBufferBackedInputStream(streamBuf);
	}

	private static int readU2(ByteBuffer buf) {
		return buf.getShort() & 0xFFFF;
	}

	private static String readString(ByteBuffer buf, int fileNameLen) {
		byte[] bytes = new byte[fileNameLen];
		buf.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public void close() throws IOException {
		try {
			if (fileChannel != null) {
				fileChannel.close();
			}
			if (file != null) {
				file.close();
			}
			if (fallbackZipContent != null) {
				fallbackZipContent.close();
			}
		} finally {
			fileChannel = null;
			file = null;
			byteBuffer = null;
			endOfCDStart = -2;
			fallbackZipContent = null;
		}
	}

	public File getZipFile() {
		return zipFile;
	}

	@Override
	public String toString() {
		return "JadxZipParser{" + zipFile + '}';
	}
}
