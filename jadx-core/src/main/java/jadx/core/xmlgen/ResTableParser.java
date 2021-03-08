package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResTableParser extends CommonBinaryParser {
	private static final Logger LOG = LoggerFactory.getLogger(ResTableParser.class);

	private static final Pattern VALID_RES_KEY_PATTERN = Pattern.compile("[\\w\\d_]+");

	private static final class PackageChunk {
		private final int id;
		private final String name;
		private final String[] typeStrings;
		private final String[] keyStrings;

		private PackageChunk(int id, String name, String[] typeStrings, String[] keyStrings) {
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

		public String[] getTypeStrings() {
			return typeStrings;
		}

		public String[] getKeyStrings() {
			return keyStrings;
		}
	}

	/**
	 * No renaming, pattern checking or name generation. Required for res-map.txt building
	 */
	private final boolean useRawResName;
	private final RootNode root;
	private final ResourceStorage resStorage = new ResourceStorage();
	private String[] strings;

	public ResTableParser(RootNode root) {
		this(root, false);
	}

	public ResTableParser(RootNode root, boolean useRawResNames) {
		this.root = root;
		this.useRawResName = useRawResNames;
	}

	public void decode(InputStream inputStream) throws IOException {
		is = new ParserStream(inputStream);
		decodeTableChunk();
		resStorage.finish();
	}

	public ResContainer decodeFiles(InputStream inputStream) throws IOException {
		decode(inputStream);

		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resGen = new ResXmlGen(resStorage, vp);

		ICodeInfo content = XmlGenUtils.makeXmlDump(root.makeCodeWriter(), resStorage);
		List<ResContainer> xmlFiles = resGen.makeResourcesXml();
		return ResContainer.resourceTable("res", xmlFiles, content);
	}

	public ResourceStorage getResStorage() {
		return resStorage;
	}

	public String[] getStrings() {
		return strings;
	}

	void decodeTableChunk() throws IOException {
		is.checkInt16(RES_TABLE_TYPE, "Not a table chunk");
		is.checkInt16(0x000c, "Unexpected table header size");
		/* int size = */
		is.readInt32();
		int pkgCount = is.readInt32();

		strings = parseStringPool();
		for (int i = 0; i < pkgCount; i++) {
			parsePackage();
		}
	}

	private PackageChunk parsePackage() throws IOException {
		long start = is.getPos();
		is.checkInt16(RES_TABLE_PACKAGE_TYPE, "Not a table chunk");
		int headerSize = is.readInt16();
		if (headerSize != 0x011c && headerSize != 0x0120) {
			die("Unexpected package header size");
		}
		long size = is.readUInt32();
		long endPos = start + size;

		int id = is.readInt32();
		String name = is.readString16Fixed(128);

		long typeStringsOffset = start + is.readInt32();
		/* int lastPublicType = */
		is.readInt32();
		long keyStringsOffset = start + is.readInt32();
		/* int lastPublicKey = */
		is.readInt32();
		if (headerSize == 0x0120) {
			/* int typeIdOffset = */
			is.readInt32();
		}

		String[] typeStrings = null;
		if (typeStringsOffset != 0) {
			is.skipToPos(typeStringsOffset, "Expected typeStrings string pool");
			typeStrings = parseStringPool();
		}
		String[] keyStrings = null;
		if (keyStringsOffset != 0) {
			is.skipToPos(keyStringsOffset, "Expected keyStrings string pool");
			keyStrings = parseStringPool();
			deobfKeyStrings(keyStrings);
		}

		PackageChunk pkg = new PackageChunk(id, name, typeStrings, keyStrings);
		resStorage.setAppPackage(name);

		while (is.getPos() < endPos) {
			long chunkStart = is.getPos();
			int type = is.readInt16();
			if (type == RES_NULL_TYPE) {
				continue;
			}
			if (type == RES_TABLE_TYPE_SPEC_TYPE) {
				parseTypeSpecChunk();
			} else if (type == RES_TABLE_TYPE_TYPE) {
				parseTypeChunk(chunkStart, pkg);
			}
		}
		return pkg;
	}

	private void deobfKeyStrings(String[] keyStrings) {
		int keysCount = keyStrings.length;
		if (root.getArgs().isRenamePrintable()) {
			for (int i = 0; i < keysCount; i++) {
				String keyString = keyStrings[i];
				if (!NameMapper.isAllCharsPrintable(keyString)) {
					keyStrings[i] = makeNewKeyName(i);
				}
			}
		}
		if (root.getArgs().isRenameValid()) {
			Set<String> keySet = new HashSet<>(keysCount);
			for (int i = 0; i < keysCount; i++) {
				String keyString = keyStrings[i];
				boolean isNew = keySet.add(keyString);
				if (!isNew) {
					keyStrings[i] = makeNewKeyName(i);
				}
			}
		}
	}

	private String makeNewKeyName(int idx) {
		return "JADX_DEOBF_" + idx;
	}

	@SuppressWarnings("unused")
	private void parseTypeSpecChunk() throws IOException {
		is.checkInt16(0x0010, "Unexpected type spec header size");
		/* int size = */
		is.readInt32();

		int id = is.readInt8();
		is.skip(3);
		int entryCount = is.readInt32();
		for (int i = 0; i < entryCount; i++) {
			int entryFlag = is.readInt32();
		}
	}

	private void parseTypeChunk(long start, PackageChunk pkg) throws IOException {
		/* int headerSize = */
		is.readInt16();
		/* int size = */
		is.readInt32();

		int id = is.readInt8();
		is.checkInt8(0, "type chunk, res0");
		is.checkInt16(0, "type chunk, res1");
		int entryCount = is.readInt32();
		long entriesStart = start + is.readInt32();

		EntryConfig config = parseConfig();

		if (config.isInvalid) {
			String typeName = pkg.getTypeStrings()[id - 1];
			LOG.warn("Invalid config flags detected: {}{}", typeName, config.getQualifiers());
		}

		int[] entryIndexes = new int[entryCount];
		for (int i = 0; i < entryCount; i++) {
			entryIndexes[i] = is.readInt32();
		}

		is.checkPos(entriesStart, "Expected entry start");
		for (int i = 0; i < entryCount; i++) {
			if (entryIndexes[i] != NO_ENTRY) {
				parseEntry(pkg, id, i, config.getQualifiers());
			}
		}
	}

	private void parseEntry(PackageChunk pkg, int typeId, int entryId, String config) throws IOException {
		int size = is.readInt16();
		int flags = is.readInt16();
		int key = is.readInt32();
		if (key == -1) {
			return;
		}

		int resRef = pkg.getId() << 24 | typeId << 16 | entryId;
		String typeName = pkg.getTypeStrings()[typeId - 1];
		String origKeyName = pkg.getKeyStrings()[key];
		ResourceEntry newResEntry = new ResourceEntry(resRef, pkg.getName(), typeName, getResName(resRef, origKeyName), config);
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

		if ((flags & FLAG_COMPLEX) != 0 || size == 16) {
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
		resStorage.add(newResEntry);
	}

	private String getResName(int resRef, String origKeyName) {
		if (this.useRawResName) {
			return origKeyName;
		}
		String renamedKey = resStorage.getRename(resRef);
		if (renamedKey != null) {
			return renamedKey;
		}
		if (VALID_RES_KEY_PATTERN.matcher(origKeyName).matches()) {
			return origKeyName;
		}
		FieldNode constField = root.getConstValues().getGlobalConstFields().get(resRef);
		if (constField != null) {
			constField.add(AFlag.DONT_RENAME);
			return constField.getName();
		}
		// Making sure origKeyName compliant with resource file name rules
		Matcher m = VALID_RES_KEY_PATTERN.matcher(origKeyName);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (m.find()) {
			if (!first) {
				sb.append("_");
			}
			sb.append(m.group());
			first = false;
		}
		// autogenerate key name, appended with cleaned origKeyName to be human-friendly
		String newResName = "res_" + resRef;
		String cleanedResName = sb.toString();
		if (!cleanedResName.isEmpty()) {
			newResName += "_" + cleanedResName.toLowerCase();
		}
		return newResName;
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
		if (size < 28) {
			throw new IOException("Config size < 28");
		}

		short mcc = (short) is.readInt16();
		short mnc = (short) is.readInt16();

		char[] language = unpackLocaleOrRegion((byte) is.readInt8(), (byte) is.readInt8(), 'a');
		char[] country = unpackLocaleOrRegion((byte) is.readInt8(), (byte) is.readInt8(), '0');

		byte orientation = (byte) is.readInt8();
		byte touchscreen = (byte) is.readInt8();
		int density = is.readInt16();

		byte keyboard = (byte) is.readInt8();
		byte navigation = (byte) is.readInt8();
		byte inputFlags = (byte) is.readInt8();
		is.readInt8(); // inputPad0

		short screenWidth = (short) is.readInt16();
		short screenHeight = (short) is.readInt16();

		short sdkVersion = (short) is.readInt16();
		is.readInt16(); // minorVersion must always be 0

		byte screenLayout = 0;
		byte uiMode = 0;
		short smallestScreenWidthDp = 0;
		if (size >= 32) {
			screenLayout = (byte) is.readInt8();
			uiMode = (byte) is.readInt8();
			smallestScreenWidthDp = (short) is.readInt16();
		}

		short screenWidthDp = 0;
		short screenHeightDp = 0;
		if (size >= 36) {
			screenWidthDp = (short) is.readInt16();
			screenHeightDp = (short) is.readInt16();
		}

		char[] localeScript = null;
		char[] localeVariant = null;
		if (size >= 48) {
			localeScript = readScriptOrVariantChar(4).toCharArray();
			localeVariant = readScriptOrVariantChar(8).toCharArray();
		}

		byte screenLayout2 = 0;
		byte colorMode = 0;
		if (size >= 52) {
			screenLayout2 = (byte) is.readInt8();
			colorMode = (byte) is.readInt8();
			is.readInt16(); // reserved padding
		}

		is.skipToPos(start + size, "Config skip trailing bytes");

		return new EntryConfig(mcc, mnc, language, country,
				orientation, touchscreen, density, keyboard, navigation,
				inputFlags, screenWidth, screenHeight, sdkVersion,
				screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
				screenHeightDp, localeScript, localeVariant, screenLayout2,
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
		long start = is.getPos();
		StringBuilder sb = new StringBuilder(16);
		for (int i = 0; i < length; i++) {
			short ch = (short) is.readInt8();
			if (ch == 0) {
				break;
			}
			sb.append((char) ch);
		}
		is.skipToPos(start + length, "readScriptOrVariantChar");
		return sb.toString();
	}
}
