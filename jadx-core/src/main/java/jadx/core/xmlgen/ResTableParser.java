package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.CodeWriter;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResTableParser extends CommonBinaryParser {

	private static final Logger LOG = LoggerFactory.getLogger(ResTableParser.class);

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

	private String[] strings;
	private final ResourceStorage resStorage = new ResourceStorage();

	public void decode(InputStream inputStream) throws IOException {
		is = new ParserStream(inputStream);
		decodeTableChunk();
		resStorage.finish();
	}

	public ResContainer decodeFiles(InputStream inputStream) throws IOException {
		decode(inputStream);

		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		ResXmlGen resGen = new ResXmlGen(resStorage, vp);

		ResContainer res = ResContainer.multiFile("res");
		res.setContent(makeXmlDump());
		res.getSubFiles().addAll(resGen.makeResourcesXml());
		return res;
	}

	public CodeWriter makeDump() {
		CodeWriter writer = new CodeWriter();
		writer.add("app package: ").add(resStorage.getAppPackage());
		writer.startLine();

		ValuesParser vp = new ValuesParser(strings, resStorage.getResourcesNames());
		for (ResourceEntry ri : resStorage.getResources()) {
			writer.startLine(ri + ": " + vp.getValueString(ri));
		}
		writer.finish();
		return writer;
	}

	public CodeWriter makeXmlDump() {
		CodeWriter writer = new CodeWriter();
		writer.startLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.startLine("<resources>");
		writer.incIndent();

		Set<String> addedValues = new HashSet<>();
		for (ResourceEntry ri : resStorage.getResources()) {
			if (addedValues.add(ri.getTypeName() + "." + ri.getKeyName())) {
				String format = String.format("<public type=\"%s\" name=\"%s\" id=\"%s\" />",
						ri.getTypeName(), ri.getKeyName(), ri.getId());
				writer.startLine(format);
			}
		}
		writer.decIndent();
		writer.startLine("</resources>");
		writer.finish();
		return writer;
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
		/*int size = */
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
		}

		PackageChunk pkg = new PackageChunk(id, name, typeStrings, keyStrings);
		//if (id == 0x7F) {
		resStorage.setAppPackage(name);
		//}

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

	private void parseTypeSpecChunk() throws IOException {
		is.checkInt16(0x0010, "Unexpected type spec header size");
		/*int size = */
		is.readInt32();

		int id = is.readInt8();
		is.skip(3);
		int entryCount = is.readInt32();
		for (int i = 0; i < entryCount; i++) {
			int entryFlag = is.readInt32();
		}
	}

	private void parseTypeChunk(long start, PackageChunk pkg) throws IOException {
		/*int headerSize = */
		is.readInt16();
		/*int size = */
		is.readInt32();

		int id = is.readInt8();
		is.checkInt8(0, "type chunk, res0");
		is.checkInt16(0, "type chunk, res1");
		int entryCount = is.readInt32();
		long entriesStart = start + is.readInt32();

		EntryConfig config = parseConfig();

		int[] entryIndexes = new int[entryCount];
		for (int i = 0; i < entryCount; i++) {
			entryIndexes[i] = is.readInt32();
		}

		is.checkPos(entriesStart, "Expected entry start");
		for (int i = 0; i < entryCount; i++) {
			if (entryIndexes[i] != NO_ENTRY) {
				parseEntry(pkg, id, i, config);
			}
		}
	}

	private void parseEntry(PackageChunk pkg, int typeId, int entryId, EntryConfig config) throws IOException {
		/* int size = */
		is.readInt16();
		int flags = is.readInt16();
		int key = is.readInt32();

		int resRef = pkg.getId() << 24 | typeId << 16 | entryId;
		String typeName = pkg.getTypeStrings()[typeId - 1];
		String keyName = pkg.getKeyStrings()[key];
		ResourceEntry ri = new ResourceEntry(resRef, pkg.getName(), typeName, keyName);
		ri.setConfig(config);

		if ((flags & FLAG_COMPLEX) == 0) {
			ri.setSimpleValue(parseValue());
		} else {
			int parentRef = is.readInt32();
			ri.setParentRef(parentRef);
			int count = is.readInt32();
			List<RawNamedValue> values = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				values.add(parseValueMap());
			}
			ri.setNamedValues(values);
		}
		resStorage.add(ri);
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

		EntryConfig config = new EntryConfig();

		is.readInt16(); //mcc
		is.readInt16(); //mnc

		config.setLanguage(parseLocale());
		config.setCountry(parseLocale());

		int orientation = is.readInt8();
		int touchscreen = is.readInt8();
		int density = is.readInt16();

		if (density != 0) {
			config.setDensity(parseDensity(density));
		}

		is.readInt8(); // keyboard
		is.readInt8(); // navigation
		is.readInt8(); // inputFlags
		is.readInt8(); // inputPad0

		int screenWidth = is.readInt16();
		int screenHeight = is.readInt16();

		if (screenWidth != 0 && screenHeight != 0) {
			config.setScreenSize(screenWidth + "x" + screenHeight);
		}

		int sdkVersion = is.readInt16();

		if (sdkVersion != 0) {
			config.setSdkVersion("v" + sdkVersion);
		}

		int minorVersion = is.readInt16();

		int screenLayout = is.readInt8();
		int uiMode = is.readInt8();
		int smallestScreenWidthDp = is.readInt16();

		int screenWidthDp = is.readInt16();
		int screenHeightDp = is.readInt16();

		if (screenLayout != 0) {
			config.setScreenLayout(parseScreenLayout(screenLayout));
		}

		if (smallestScreenWidthDp != 0) {
			config.setSmallestScreenWidthDp("sw" + smallestScreenWidthDp + "dp");
		}

		if (orientation != 0) {
			config.setOrientation(parseOrientation(orientation));
		}

		if (screenWidthDp != 0) {
			config.setScreenWidthDp("w" + screenWidthDp + "dp");
		}

		if (screenHeightDp != 0) {
			config.setScreenHeightDp("h" + screenHeightDp + "dp");
		}

		is.skipToPos(start + size, "Skip config parsing");
		return config;
	}

	private String parseOrientation(int orientation) {
		if (orientation == 1) {
			return "port";
		} else if (orientation == 2) {
			return "land";
		} else {
			return "o" + orientation;
		}
	}

	private String parseScreenLayout(int screenLayout) {
		switch (screenLayout) {
			case 1:
				return "small";
			case 2:
				return "normal";
			case 3:
				return "large";
			case 4:
				return "xlarge";
			case 64:
				return "ldltr";
			case 128:
				return "ldrtl";
			default:
				return "sl" + screenLayout;
		}
	}

	private String parseDensity(int density) {
		if (density == 120) {
			return "ldpi";
		} else if (density == 160) {
			return "mdpi";
		} else if (density == 240) {
			return "hdpi";
		} else if (density == 320) {
			return "xhdpi";
		} else if (density == 480) {
			return "xxhdpi";
		} else if (density == 640) {
			return "xxxhdpi";
		} else {
			return density + "dpi";
		}
	}

	private String parseLocale() throws IOException {
		int b1 = is.readInt8();
		int b2 = is.readInt8();
		String str = null;
		if (b1 != 0 && b2 != 0) {
			if ((b1 & 0x80) == 0) {
				str = new String(new char[]{(char) b1, (char) b2});
			} else {
				LOG.warn("TODO: parse locale: 0x{}{}", Integer.toHexString(b1), Integer.toHexString(b2));
			}
		}
		return str;
	}
}
