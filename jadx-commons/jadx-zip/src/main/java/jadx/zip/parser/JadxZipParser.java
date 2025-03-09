package jadx.zip.parser;

import java.io.ByteArrayInputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.zip.IZipEntry;
import jadx.zip.IZipParser;
import jadx.zip.ZipContent;
import jadx.zip.ZipReaderFlags;

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
	private final Set<ZipReaderFlags> flags;
	private final boolean verify;

	private RandomAccessFile file;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;

	private int endOfCDStart = -2;

	public JadxZipParser(File zipFile, Set<ZipReaderFlags> flags) {
		this.zipFile = zipFile;
		this.flags = flags;
		this.verify = flags.contains(ZipReaderFlags.REPORT_TAMPERING);
	}

	@Override
	public ZipContent open() throws IOException {
		load();
		List<IZipEntry> entries;
		if (flags.contains(ZipReaderFlags.IGNORE_CENTRAL_DIR_ENTRIES)) {
			entries = searchLocalFileHeaders();
		} else {
			entries = loadFromCentralDirs();
		}
		return new ZipContent(this, entries);
	}

	public boolean canOpen() throws IOException {
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
			byteBuffer = ByteBuffer.wrap(bytes);
			file.close();
			file = null;
		} else {
			// for big files - use a memory mapped file
			fileChannel = file.getChannel();
			byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		}
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		if (byteBuffer.limit() != fileLen) {
			throw new IOException("Zip file incorrect limit");
		}
	}

	private List<IZipEntry> searchLocalFileHeaders() {
		List<IZipEntry> entries = new ArrayList<>();
		while (true) {
			int start = searchEntryStart();
			if (start == -1) {
				return entries;
			}
			entries.add(loadFileEntry(start));
		}
	}

	private List<IZipEntry> loadFromCentralDirs() throws IOException {
		int eocdStart = searchEndOfCDStart();
		if (eocdStart < 0) {
			throw new RuntimeException("End of central directory not found");
		}
		ByteBuffer buf = byteBuffer;
		buf.position(eocdStart + 10);
		int entriesCount = readU2(buf);
		buf.position(eocdStart + 16);
		int cdOffset = buf.getInt();

		List<IZipEntry> entries = new ArrayList<>(entriesCount);
		buf.position(cdOffset);
		for (int i = 0; i < entriesCount; i++) {
			entries.add(loadCDEntry());
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

	InputStream getInputStream(JadxZipEntry entry) {
		return new ByteArrayInputStream(getBytes(entry));
	}

	synchronized byte[] getBytes(JadxZipEntry entry) {
		int compressMethod = entry.getCompressMethod();
		if (verify) {
			if (compressMethod == 0) {
				if (entry.getCompressedSize() != entry.getUncompressedSize()) {
					LOG.warn("Not equal sizes for STORE method: compressed: {}, uncompressed: {}, entry: {}",
							entry.getCompressedSize(), entry.getUncompressedSize(), entry);
				}
			} else if (compressMethod != 8) {
				LOG.warn("Unknown compress method: {} in entry: {}", compressMethod, entry);
			}
		}
		if (compressMethod == 8) {
			try {
				return ZipDeflate.decompressEntryToBytes(byteBuffer, entry);
			} catch (Exception e) {
				if (isEncrypted(entry)) {
					throw new RuntimeException("Entry is encrypted, failed to decompress: " + entry, e);
				}
				throw new RuntimeException("Failed to decompress zip entry: " + entry + ", error: " + e.getMessage(), e);
			}
		}
		return bufferToBytes(entry.getDataStart(), (int) entry.getUncompressedSize());
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

	byte[] bufferToBytes(int start, int size) {
		ByteBuffer buf = byteBuffer;
		byte[] data = new byte[size];
		buf.position(start);
		buf.get(data);
		return data;
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
		} finally {
			fileChannel = null;
			file = null;
			byteBuffer = null;
			endOfCDStart = -2;
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
