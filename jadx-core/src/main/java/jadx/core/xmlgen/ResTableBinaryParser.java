package jadx.core.xmlgen;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.args.ResourceNameSource;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.IFieldInfoRef;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.BetterName;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResTableBinaryParser extends CommonBinaryParser implements IResTableParser {
	private static final Logger LOG = LoggerFactory.getLogger(ResTableBinaryParser.class);

	private static final class PackageChunk {
		private final int id;
		private final String name;
		private final BinaryXMLStrings typeStrings;
		private final BinaryXMLStrings keyStrings;

		private PackageChunk(int id, String name, BinaryXMLStrings typeStrings, BinaryXMLStrings keyStrings) {
			this.id = id;
			this.name = name;
			this.typeStrings = typeStrings;
			this.keyStrings = keyStrings;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public BinaryXMLStrings getTypeStrings() {
			return typeStrings;
		}

		public BinaryXMLStrings getKeyStrings() {
			return keyStrings;
		}
	}

	/**
	 * No renaming, pattern checking or name generation. Required for res-map.txt building
	 */
	private final boolean useRawResName;
	private final RootNode root;

	private ResourceStorage resStorage;
	private BinaryXMLStrings strings;

	public ResTableBinaryParser(RootNode root) {
		this(root, false);
	}

	public ResTableBinaryParser(RootNode root, boolean useRawResNames) {
		this.root = root;
		this.useRawResName = useRawResNames;
	}

	@Override
	public void decode(InputStream inputStream) throws IOException {
		long start = System.currentTimeMillis();
		is = new ParserStream(new BufferedInputStream(inputStream, 32768));
		resStorage = new ResourceStorage(root.getArgs().getSecurity());
		decodeTableChunk();
		resStorage.finish();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Resource table parsed: size: {}, time: {}ms",
					resStorage.size(), System.currentTimeMillis() - start);
		}
	}

	@Override
	public ResContainer decodeFiles() {
		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resGen = new ResXmlGen(resStorage, vp, root.initManifestAttributes());

		ICodeInfo content = XmlGenUtils.makeXmlDump(root.makeCodeWriter(), resStorage);
		List<ResContainer> xmlFiles = resGen.makeResourcesXml(root.getArgs());
		return ResContainer.resourceTable("res", xmlFiles, content);
	}

	void decodeTableChunk() throws IOException {
		is.checkInt16(RES_TABLE_TYPE, "Not a table chunk");
		is.checkInt16(0x000c, "Unexpected table header size");
		int size = is.readInt32();
		int pkgCount = is.readInt32();

		int pkgNum = 0;
		while (is.getPos() < size) {
			long chuckStart = is.getPos();
			int type = is.readInt16();
			int headerSize = is.readInt16();
			long chunkSize = is.readUInt32();
			long chunkEnd = chuckStart + chunkSize;
			switch (type) {
				case RES_NULL_TYPE:
					// skip
					break;

				case RES_STRING_POOL_TYPE:
					strings = parseStringPoolNoSize(chuckStart, chunkEnd);
					break;

				case RES_TABLE_PACKAGE_TYPE:
					parsePackage(chuckStart, headerSize, chunkEnd);
					pkgNum++;
					break;
			}
			is.skipToPos(chunkEnd, "Skip to table chunk end");
		}
		if (pkgNum != pkgCount) {
			LOG.warn("Unexpected package chunks, read: {}, expected: {}", pkgNum, pkgCount);
		}
	}

	private void parsePackage(long pkgChunkStart, int headerSize, long pkgChunkEnd) throws IOException {
		if (headerSize < 0x011c) {
			die("Package header size too small");
			return;
		}
		int id = is.readInt32();
		String name = is.readString16Fixed(128);
		long typeStringsOffset = pkgChunkStart + is.readInt32();
		int lastPublicType = is.readInt32();
		long keyStringsOffset = pkgChunkStart + is.readInt32();
		int lastPublicKey = is.readInt32();
		if (headerSize >= 0x0120) {
			int typeIdOffset = is.readInt32();
		}
		is.skipToPos(pkgChunkStart + headerSize, "package header end");

		BinaryXMLStrings typeStrings = null;
		if (typeStringsOffset != 0) {
			is.skipToPos(typeStringsOffset, "Expected typeStrings string pool");
			typeStrings = parseStringPool();
		}
		BinaryXMLStrings keyStrings = null;
		if (keyStringsOffset != 0) {
			is.skipToPos(keyStringsOffset, "Expected keyStrings string pool");
			keyStrings = parseStringPool();
		}

		PackageChunk pkg = new PackageChunk(id, name, typeStrings, keyStrings);
		resStorage.setAppPackage(name);

		while (is.getPos() < pkgChunkEnd) {
			long chunkStart = is.getPos();
			int type = is.readInt16();
			LOG.trace("res package chunk start at {} type {}", chunkStart, type);
			switch (type) {
				case RES_NULL_TYPE:
					LOG.info("Null chunk type encountered at offset {}", chunkStart);
					break;
				case RES_TABLE_TYPE_TYPE: // 0x0201
					parseTypeChunk(chunkStart, pkg);
					break;
				case RES_TABLE_TYPE_SPEC_TYPE: // 0x0202
					parseTypeSpecChunk(chunkStart);
					break;
				case RES_TABLE_TYPE_LIBRARY: // 0x0203
					parseLibraryTypeChunk(chunkStart);
					break;
				case RES_TABLE_TYPE_OVERLAY: // 0x0204
					parseOverlayTypeChunk(chunkStart);
					break;
				case RES_TABLE_TYPE_OVERLAY_POLICY: // 0x0205
					throw new IOException(
							String.format("Encountered unsupported chunk type RES_TABLE_TYPE_OVERLAY_POLICY at offset 0x%x ", chunkStart));
				case RES_TABLE_TYPE_STAGED_ALIAS: // 0x0206
					parseStagedAliasChunk(chunkStart);
					break;
				default:
					LOG.warn("Unknown chunk type {} encountered at offset {}", type, chunkStart);
			}
		}
	}

	@SuppressWarnings("unused")
	private void parseTypeSpecChunk(long chunkStart) throws IOException {
		is.checkInt16(0x0010, "Unexpected type spec header size");
		int chunkSize = is.readInt32();
		long expectedEndPos = chunkStart + chunkSize;

		int id = is.readInt8();
		is.skip(3);
		int entryCount = is.readInt32();
		for (int i = 0; i < entryCount; i++) {
			int entryFlag = is.readInt32();
		}
		if (is.getPos() != expectedEndPos) {
			throw new IOException(String.format("Error reading type spec chunk at offset 0x%x", chunkStart));
		}
	}

	private void parseLibraryTypeChunk(long chunkStart) throws IOException {
		LOG.trace("parsing library type chunk starting at offset {}", chunkStart);
		is.checkInt16(12, "Unexpected header size");
		int chunkSize = is.readInt32();
		long expectedEndPos = chunkStart + chunkSize;
		int count = is.readInt32();
		for (int i = 0; i < count; i++) {
			int packageId = is.readInt32();
			String packageName = is.readString16Fixed(128);
			LOG.info("Found resource shared library {}, pkgId: {}", packageName, packageId);
			if (is.getPos() > expectedEndPos) {
				throw new IOException("reading after chunk end");
			}
		}
		if (is.getPos() != expectedEndPos) {
			throw new IOException(String.format("Error reading library chunk at offset 0x%x", chunkStart));
		}
	}

	/**
	 * Parse an <code>ResTable_type</code> (except for the 2 bytes <code>uint16_t</code>
	 * from <code>ResChunk_header</code>).
	 *
	 * @see <a href=
	 *      "https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/include/androidfw/ResourceTypes.h"></a>ResourceTypes.h</a>
	 */
	private void parseTypeChunk(long start, PackageChunk pkg) throws IOException {
		/* int headerSize = */
		is.readInt16();
		/* int size = */
		long chunkSize = is.readUInt32();
		long chunkEnd = start + chunkSize;
		is.mark((int) chunkSize);

		// The type identifier this chunk is holding. Type IDs start at 1 (corresponding
		// to the value of the type bits in a resource identifier). 0 is invalid.
		int id = is.readInt8();
		String typeName = pkg.getTypeStrings().get(id - 1);

		int flags = is.readInt8();
		boolean isSparse = (flags & FLAG_SPARSE) != 0;
		boolean isOffset16 = (flags & FLAG_OFFSET16) != 0;

		is.readInt16(); // ignore reserved value - should be zero but in some apps it is not zero; see #2402
		int entryCount = is.readInt32();
		long entriesStart = start + is.readInt32();

		EntryConfig config = parseConfig();

		if (config.isInvalid) {
			LOG.warn("Invalid config flags detected: {}{}", typeName, config.getQualifiers());
		}

		List<EntryOffset> offsets = new ArrayList<>(entryCount);
		if (isSparse) {
			for (int i = 0; i < entryCount; i++) {
				int idx = is.readInt16();
				int offset = is.readInt16() * 4; // The offset in ResTable_sparseTypeEntry::offset is stored divided by 4.
				offsets.add(new EntryOffset(idx, offset));
			}
		} else if (isOffset16) {
			for (int i = 0; i < entryCount; i++) {
				int offset = is.readInt16();
				if (offset != 0xFFFF) {
					offsets.add(new EntryOffset(i, offset * 4));
				}
			}
		} else {
			for (int i = 0; i < entryCount; i++) {
				offsets.add(new EntryOffset(i, is.readInt32()));
			}
		}
		is.skipToPos(entriesStart, "Failed to skip to entries start");
		int ignoredEoc = 0; // ignored entries because they are located after end of chunk
		for (EntryOffset entryOffset : offsets) {
			int offset = entryOffset.getOffset();
			if (offset == NO_ENTRY) {
				continue;
			}
			long entryStartOffset = entriesStart + offset;
			if (entryStartOffset >= chunkEnd) {
				// Certain resource obfuscated apps like com.facebook.orca have more entries defined
				// than actually fit into the chunk size -> ignore this entry
				ignoredEoc++;
				// LOG.debug("Pos is after chunk end: {} end {}", entryStartOffset, chunkEnd);
				continue;
			}
			if (entryStartOffset < is.getPos()) {
				// workaround for issue #2343: if the entryStartOffset is located before our current position
				is.reset();
			}
			int index = entryOffset.getIdx();
			is.skipToPos(entryStartOffset, "Expected start of entry " + index);
			parseEntry(pkg, id, index, config.getQualifiers());
		}
		if (ignoredEoc > 0) {
			// invalid = data offset is after the chunk end
			LOG.warn("{} entries of type {} has been ignored (invalid offset)", ignoredEoc, typeName);
		}
		is.skipToPos(chunkEnd, "End of chunk");
	}

	private static class EntryOffset {
		private final int idx;
		private final int offset;

		private EntryOffset(int idx, int offset) {
			this.idx = idx;
			this.offset = offset;
		}

		public int getIdx() {
			return idx;
		}

		public int getOffset() {
			return offset;
		}
	}

	private void parseOverlayTypeChunk(long chunkStart) throws IOException {
		LOG.trace("parsing overlay type chunk starting at offset {}", chunkStart);
		// read ResTable_overlayable_header
		/* headerSize = */
		is.readInt16(); // usually 1032 bytes
		int chunkSize = is.readInt32(); // e.g. 1056 bytes
		long expectedEndPos = chunkStart + chunkSize;
		String name = is.readString16Fixed(256); // 512 bytes
		String actor = is.readString16Fixed(256); // 512 bytes
		LOG.trace("Overlay header data: name={} actor={}", name, actor);
		// skip: ResTable_overlayable_policy_header + ResTable_ref * x
		is.skipToPos(expectedEndPos, "overlay chunk end");
	}

	private void parseStagedAliasChunk(long chunkStart) throws IOException {
		// read ResTable_staged_alias_header
		LOG.trace("parsing staged alias chunk starting at offset {}", chunkStart);
		/* headerSize = */
		is.readInt16();
		int chunkSize = is.readInt32();
		long expectedEndPos = chunkStart + chunkSize;
		int count = is.readInt32();

		for (int i = 0; i < count; i++) {
			// read ResTable_staged_alias_entry
			int stagedResId = is.readInt32();
			int finalizedResId = is.readInt32();
			LOG.debug("Staged alias: stagedResId {} finalizedResId {}", stagedResId, finalizedResId);
		}
		is.skipToPos(expectedEndPos, "staged alias chunk end");
	}

	private void parseEntry(PackageChunk pkg, int typeId, int entryId, String config) throws IOException {
		int size = is.readInt16();
		int flags = is.readInt16();
		boolean isComplex = (flags & FLAG_COMPLEX) != 0;
		boolean isCompact = (flags & FLAG_COMPACT) != 0;

		int key = isCompact ? size : is.readInt32();
		if (key == -1) {
			return;
		}

		int resRef = pkg.getId() << 24 | typeId << 16 | entryId;
		String typeName = pkg.getTypeStrings().get(typeId - 1);
		String origKeyName = pkg.getKeyStrings().get(key);

		ResourceEntry newResEntry = buildResourceEntry(pkg, config, resRef, typeName, origKeyName);
		if (isCompact) {
			int dataType = flags >> 8;
			int data = is.readInt32();
			newResEntry.setSimpleValue(new RawValue(dataType, data));
		} else if (isComplex || size == 16) {
			int parentRef = is.readInt32();
			int count = is.readInt32();
			newResEntry.setParentRef(parentRef);
			List<RawNamedValue> values = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				values.add(parseValueMap());
			}
			newResEntry.setNamedValues(values);
		} else {
			newResEntry.setSimpleValue(parseValue());
		}
	}

	private static final ResourceEntry STUB_ENTRY = new ResourceEntry(-1, "stub", "stub", "stub", "");

	private ResourceEntry buildResourceEntry(PackageChunk pkg, String config, int resRef, String typeName, String origKeyName) {
		if (!root.getArgs().getSecurity().isValidEntryName(origKeyName)) {
			// malicious entry, ignore it
			// can't return null here, return stub without adding it to storage
			return STUB_ENTRY;
		}

		ResourceEntry newResEntry;
		if (useRawResName) {
			newResEntry = new ResourceEntry(resRef, pkg.getName(), typeName, origKeyName, config);
		} else {
			String resName = getResName(resRef, origKeyName);
			newResEntry = new ResourceEntry(resRef, pkg.getName(), typeName, resName, config);
			ResourceEntry prevResEntry = resStorage.searchEntryWithSameName(newResEntry);
			if (prevResEntry != null) {
				newResEntry = newResEntry.copyWithId();

				// rename also previous entry for consistency
				ResourceEntry replaceForPrevEntry = prevResEntry.copyWithId();
				resStorage.replace(prevResEntry, replaceForPrevEntry);
				resStorage.addRename(replaceForPrevEntry);
			}
			if (!Objects.equals(origKeyName, newResEntry.getKeyName())) {
				resStorage.addRename(newResEntry);
			}
		}

		resStorage.add(newResEntry);
		return newResEntry;
	}

	private String getResName(int resRef, String origKeyName) {
		if (this.useRawResName) {
			return origKeyName;
		}
		String renamedKey = resStorage.getRename(resRef);
		if (renamedKey != null) {
			return renamedKey;
		}

		IFieldInfoRef fldRef = root.getConstValues().getGlobalConstFields().get(resRef);
		FieldNode constField = fldRef instanceof FieldNode ? (FieldNode) fldRef : null;

		String newResName = getNewResName(resRef, origKeyName, constField);
		if (!origKeyName.equals(newResName)) {
			resStorage.addRename(resRef, newResName);
		}

		if (constField != null) {
			final String newFieldName = ResNameUtils.convertToRFieldName(newResName);
			constField.rename(newFieldName);
			constField.add(AFlag.DONT_RENAME);
		}

		return newResName;
	}

	private String getNewResName(int resRef, String origKeyName, @Nullable FieldNode constField) {
		String newResName;
		if (constField == null || constField.getTopParentClass().isSynthetic()) {
			newResName = origKeyName;
		} else {
			newResName = getBetterName(root.getArgs().getResourceNameSource(), origKeyName, constField.getName());
		}

		if (root.getArgs().isRenameValid()) {
			final boolean allowNonPrintable = !root.getArgs().isRenamePrintable();
			newResName = ResNameUtils.sanitizeAsResourceName(newResName, String.format("_res_0x%08x", resRef), allowNonPrintable);
		}

		return newResName;
	}

	public static String getBetterName(ResourceNameSource nameSource, String resName, String codeName) {
		switch (nameSource) {
			case AUTO:
				return BetterName.getBetterResourceName(resName, codeName);
			case RESOURCES:
				return resName;
			case CODE:
				return codeName;

			default:
				throw new JadxRuntimeException("Unexpected ResourceNameSource value: " + nameSource);
		}
	}

	private RawNamedValue parseValueMap() throws IOException {
		int nameRef = is.readInt32();
		return new RawNamedValue(nameRef, parseValue());
	}

	private RawValue parseValue() throws IOException {
		is.checkInt16(8, "value size");
		is.checkInt8(0, "value res0 not 0");
		int dataType = is.readInt8();
		int data = is.readInt32();
		return new RawValue(dataType, data);
	}

	private EntryConfig parseConfig() throws IOException {
		long start = is.getPos();
		int size = is.readInt32();
		if (size < 4) {
			throw new IOException("Config size < 4");
		}

		// Android zero fill this structure and only read the data present
		var configData = new byte[Math.max(52, size - 4)];
		is.readFully(configData, 0, size - 4);
		var configIs = new ParserStream(new ByteArrayInputStream(configData));

		short mcc = (short) configIs.readInt16();
		short mnc = (short) configIs.readInt16();

		char[] language = unpackLocaleOrRegion((byte) configIs.readInt8(), (byte) configIs.readInt8(), 'a');
		char[] country = unpackLocaleOrRegion((byte) configIs.readInt8(), (byte) configIs.readInt8(), '0');

		byte orientation = (byte) configIs.readInt8();
		byte touchscreen = (byte) configIs.readInt8();
		int density = configIs.readInt16();

		byte keyboard = (byte) configIs.readInt8();
		byte navigation = (byte) configIs.readInt8();
		byte inputFlags = (byte) configIs.readInt8();
		byte grammaticalInflection = (byte) configIs.readInt8();

		short screenWidth = (short) configIs.readInt16();
		short screenHeight = (short) configIs.readInt16();

		short sdkVersion = (short) configIs.readInt16();
		configIs.readInt16(); // minorVersion must always be 0

		byte screenLayout = (byte) configIs.readInt8();
		byte uiMode = (byte) configIs.readInt8();
		short smallestScreenWidthDp = (short) configIs.readInt16();
		short screenWidthDp = (short) configIs.readInt16();
		short screenHeightDp = (short) configIs.readInt16();

		char[] localeScript = readScriptOrVariantChar(4, configIs).toCharArray();
		char[] localeVariant = readScriptOrVariantChar(8, configIs).toCharArray();

		byte screenLayout2 = (byte) configIs.readInt8();
		byte colorMode = (byte) configIs.readInt8();
		configIs.readInt16(); // reserved padding

		is.checkPos(start + size, "Config skip trailing bytes");

		return new EntryConfig(mcc, mnc, language, country,
				orientation, touchscreen, density, keyboard, navigation,
				inputFlags, grammaticalInflection, screenWidth, screenHeight, sdkVersion,
				screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
				screenHeightDp,
				localeScript.length == 0 ? null : localeScript,
				localeVariant.length == 0 ? null : localeVariant,
				screenLayout2,
				colorMode, false, size);
	}

	private char[] unpackLocaleOrRegion(byte in0, byte in1, char base) {
		// check high bit, if so we have a packed 3 letter code
		if (((in0 >> 7) & 1) == 1) {
			int first = in1 & 0x1F;
			int second = ((in1 & 0xE0) >> 5) + ((in0 & 0x03) << 3);
			int third = (in0 & 0x7C) >> 2;

			// since this function handles languages & regions, we add the value(s) to the base char
			// which is usually 'a' or '0' depending on language or region.
			return new char[] { (char) (first + base), (char) (second + base), (char) (third + base) };
		}
		return new char[] { (char) in0, (char) in1 };
	}

	private String readScriptOrVariantChar(int length) throws IOException {
		return readScriptOrVariantChar(length, is);
	}

	private static String readScriptOrVariantChar(int length, ParserStream ps) throws IOException {
		long start = ps.getPos();
		StringBuilder sb = new StringBuilder(16);
		for (int i = 0; i < length; i++) {
			short ch = (short) ps.readInt8();
			if (ch == 0) {
				break;
			}
			sb.append((char) ch);
		}
		ps.skipToPos(start + length, "readScriptOrVariantChar");
		return sb.toString();
	}

	@Override
	public ResourceStorage getResStorage() {
		return resStorage;
	}

	@Override
	public BinaryXMLStrings getStrings() {
		return strings;
	}
}
