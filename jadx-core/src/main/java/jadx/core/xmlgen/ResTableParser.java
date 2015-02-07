package jadx.core.xmlgen;

import jadx.core.codegen.CodeWriter;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public CodeWriter decodeToCodeWriter(InputStream inputStream) throws IOException {
		decode(inputStream);

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

	public ResourceStorage getResStorage() {
		return resStorage;
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
		if (id == 0x7F) {
			resStorage.setAppPackage(name);
		}

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
		int headerSize = is.readInt16();
		if (headerSize != 0x34 && headerSize != 0x38 && headerSize != 0x44) {
			die("Unexpected type header size: 0x" + Integer.toHexString(headerSize));
		}
		/*int size =*/
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
			List<RawNamedValue> values = new ArrayList<RawNamedValue>(count);
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
		/*
		is.readInt8(); // keyboard
		is.readInt8(); // navigation
		is.readInt8(); // inputFlags
		is.readInt8(); // inputPad0

		is.readInt16(); // screenWidth
		is.readInt16(); // screenHeight

		is.readInt16(); // sdkVersion
		is.readInt16(); // minorVersion

		is.readInt8(); // screenLayout
		is.readInt8(); // uiMode
		is.readInt16(); // smallestScreenWidthDp

		is.readInt16(); // screenWidthDp
		is.readInt16(); // screenHeightDp
		*/
		is.skipToPos(start + size, "Skip config parsing");
		return config;
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
