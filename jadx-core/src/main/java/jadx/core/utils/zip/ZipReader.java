package jadx.core.utils.zip;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Simple ZIP reader implementation with a limited features to allow loading tampered files.
 * Currently, no support for:
 * - Zip64
 * - other compression methods rather than DEFLATE
 */
public final class ZipReader {
	private static final Logger LOG = LoggerFactory.getLogger(ZipReader.class);

	public static final EnumSet<Flags> DEFAULT_FLAGS = EnumSet.of(Flags.REPORT_TAMPERING);

	public enum Flags {
		/**
		 * Search all local file headers by signature without reading
		 * 'central directory' and 'end of central directory' entries
		 */
		IGNORE_CENTRAL_DIR_ENTRIES,
		/**
		 * TODO: not yet implemented
		 */
		VERIFY_CHECKSUM,

		/**
		 * Enable additional checks to verify zip data and report possible tampering
		 */
		REPORT_TAMPERING,
	}

	public static ZipContent open(File zipFile) {
		return open(zipFile, DEFAULT_FLAGS);
	}

	public static ZipContent open(File zipFile, Set<Flags> flags) {
		try {
			return new ZipReader(zipFile, flags).open();
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to open zip file: " + zipFile.getAbsolutePath());
		}
	}

	private static final byte LOCAL_FILE_HEADER_START = 0x50;
	private static final int LOCAL_FILE_HEADER_SIGN = 0x04034b50;
	private static final int CD_SIGN = 0x02014b50;
	private static final int END_OF_CD_SIGN = 0x06054b50;

	private final File zipFile;
	private final Set<Flags> flags;
	private final boolean verify;

	private RandomAccessFile file;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;

	ZipReader(File zipFile, Set<Flags> flags) {
		this.zipFile = zipFile;
		this.flags = flags;
		this.verify = flags.contains(Flags.REPORT_TAMPERING);
	}

	private ZipContent open() throws IOException {
		loadFile();
		List<ZipFileEntry> entries;
		if (flags.contains(Flags.IGNORE_CENTRAL_DIR_ENTRIES)) {
			entries = searchLocalFileHeaders();
		} else {
			entries = loadFromCentralDirs();
		}
		return new ZipContent(this, entries);
	}

	private void loadFile() throws IOException {
		file = new RandomAccessFile(zipFile, "r");
		long size = file.length();
		if (size >= Integer.MAX_VALUE) {
			throw new JadxRuntimeException("Zip file is too big");
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
			// for big files - use memory mapped file
			fileChannel = file.getChannel();
			byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		}
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		if (byteBuffer.limit() != fileLen) {
			throw new JadxRuntimeException("Zip file incorrect limit");
		}
	}

	private List<ZipFileEntry> searchLocalFileHeaders() {
		List<ZipFileEntry> entries = new ArrayList<>();
		while (true) {
			int start = searchEntryStart();
			if (start == -1) {
				return entries;
			}
			ZipFileEntry entry = loadFileEntry(start);
			entries.add(entry);
		}
	}

	private List<ZipFileEntry> loadFromCentralDirs() {
		int ecdStart = searchEndOfCDStart();
		if (ecdStart == -1) {
			throw new JadxRuntimeException("End of central directory not found");
		}
		ByteBuffer buf = byteBuffer;
		buf.position(ecdStart + 10);
		int entriesCount = buf.getShort();
		buf.position(ecdStart + 16);
		int cdOffset = buf.getInt();

		List<ZipFileEntry> entries = new ArrayList<>(entriesCount);
		buf.position(cdOffset);
		for (int i = 0; i < entriesCount; i++) {
			entries.add(loadCDEntry());
		}
		return entries;
	}

	private ZipFileEntry loadCDEntry() {
		ByteBuffer buf = byteBuffer;
		int start = buf.position();
		buf.position(start + 28);
		int fileNameLen = buf.getShort();
		int extraFieldLen = buf.getShort();
		int commentLen = buf.getShort();
		buf.position(start + 42);
		int fileEntryStart = buf.getInt();
		ZipFileEntry entry = loadFileEntry(fileEntryStart);
		if (verify) {
			compareCDAndLFH(buf, start, entry);
		}
		int entryEnd = start + 46 + fileNameLen + extraFieldLen + commentLen;
		buf.position(entryEnd); // skip to entry end
		return entry;
	}

	private static void compareCDAndLFH(ByteBuffer buf, int start, ZipFileEntry entry) {
		buf.position(start + 10);
		int comprMethod = buf.getShort();
		if (comprMethod != entry.getCompressMethod()) {
			LOG.warn("Compression method differ in CD {} and LFH {} for {}",
					comprMethod, entry.getCompressMethod(), entry.getName());
		}
		buf.position(start + 20);
		int comprSize = buf.getInt();
		int unComprSize = buf.getInt();
		if (comprSize != entry.getCompressedSize()) {
			LOG.warn("Compressed size differ in CD {} and LFH {} for {}",
					comprSize, entry.getCompressedSize(), entry.getName());
		}
		if (unComprSize != entry.getUncompressedSize()) {
			LOG.warn("Uncompressed size differ in CD {} and LFH {} for {}",
					unComprSize, entry.getUncompressedSize(), entry.getName());
		}
	}

	private ZipFileEntry loadFileEntry(int start) {
		ByteBuffer buf = byteBuffer;
		buf.position(start + 8);
		int comprMethod = buf.getShort();
		buf.position(start + 18);
		int comprSize = buf.getInt();
		int unComprSize = buf.getInt();
		int fileNameLen = buf.getShort();
		int extraFieldLen = buf.getShort();
		String fileName = readString(fileNameLen);
		int dataStart = start + 30 + fileNameLen + extraFieldLen;
		buf.position(dataStart + comprSize); // skip to entry end
		return new ZipFileEntry(this, fileName, dataStart, comprMethod, comprSize, unComprSize);
	}

	private String readString(int fileNameLen) {
		byte[] bytes = new byte[fileNameLen];
		byteBuffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private int searchEndOfCDStart() {
		ByteBuffer buf = byteBuffer;
		int pos = buf.limit() - 22;
		int minPos = pos - 0xffff;
		while (true) {
			buf.position(pos);
			int sign = buf.getInt();
			if (sign == END_OF_CD_SIGN) {
				return pos;
			}
			pos--;
			if (pos < minPos) {
				return -1;
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

	synchronized byte[] getEntryBytes(ZipFileEntry entry) {
		int compressMethod = entry.getCompressMethod();
		if (compressMethod == 8) {
			try {
				return ZipDeflate.decompressEntryToBytes(byteBuffer, entry);
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to decompress zip entry: " + entry.getName(), e);
			}
		}
		if (verify) {
			if (compressMethod == 0) {
				if (entry.getCompressedSize() != entry.getUncompressedSize()) {
					LOG.warn("Not equal sizes for STORE method: compressed: {}, uncompressed: {}",
							entry.getCompressedSize(), entry.getUncompressedSize());
				}
			} else {
				LOG.warn("Unknown compress method: {}", compressMethod);
			}
		}
		return bufferToBytes(entry.getDataStart(), entry.getUncompressedSize());
	}

	byte[] bufferToBytes(int start, int size) {
		byte[] data = new byte[size];
		byteBuffer.position(start);
		byteBuffer.get(data);
		return data;
	}

	void close() throws IOException {
		if (fileChannel != null) {
			fileChannel.close();
		}
		if (file != null) {
			file.close();
		}
	}

	@Override
	public String toString() {
		return "ZipReader{" + zipFile.getAbsolutePath() + '}';
	}
}
