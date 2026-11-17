package jadx.core.xmlgen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.RawValue;
import jadx.core.xmlgen.entry.ResourceEntry;

import static jadx.core.xmlgen.ParserConstants.FLAG_COMPLEX;
import static jadx.core.xmlgen.ParserConstants.RES_STRING_POOL_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_TABLE_PACKAGE_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_TABLE_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_TABLE_TYPE_SPEC_TYPE;
import static jadx.core.xmlgen.ParserConstants.RES_TABLE_TYPE_TYPE;
import static jadx.core.xmlgen.ParserConstants.TYPE_INT_BOOLEAN;
import static jadx.core.xmlgen.ParserConstants.UTF8_FLAG;
import static org.assertj.core.api.Assertions.assertThat;

class ResTableBinaryParserTest {

	@ParameterizedTest
	@ValueSource(ints = { 3, 8, 12 })
	void testValueSize(int size) throws IOException {
		ResourceStorage storage = parse(simpleEntry(size, 0, 0, -1));
		if (size < 8) {
			assertThat(storage.getResources()).isEmpty();
			return;
		}
		assertThat(storage.getResources()).singleElement().satisfies(entry -> {
			assertThat(entry.getId()).isEqualTo(0x7f010000);
			assertThat(entry.getTypeName()).isEqualTo("bool");
			assertThat(entry.getKeyName()).isEqualTo("flag");
			assertBool(entry.getSimpleValue(), -1);
		});
	}

	@Test
	void testReservedValueByte() throws IOException {
		ResourceStorage storage = parse(simpleEntry(8, 255, 0, -1));
		assertThat(storage.getResources()).singleElement()
				.satisfies(entry -> assertBool(entry.getSimpleValue(), -1));
	}

	@ParameterizedTest
	@ValueSource(ints = { 3, 12 })
	void testFollowingEntryOffset(int size) throws IOException {
		byte[] first = simpleEntry(size, 0, 0, -1);
		// Entry offsets, rather than the declared value size, locate subsequent entries.
		ResourceStorage storage = parse(Arrays.copyOf(first, first.length + 4), simpleEntry(8, 0, 1, 0));
		if (size < 8) {
			assertThat(storage.getResources()).extracting(ResourceEntry::getKeyName).containsExactly("other");
			assertThat(storage.getResources()).extracting(entry -> entry.getSimpleValue().getData()).containsExactly(0);
		} else {
			assertThat(storage.getResources()).extracting(ResourceEntry::getKeyName).containsExactly("flag", "other");
			assertThat(storage.getResources()).extracting(entry -> entry.getSimpleValue().getData()).containsExactly(-1, 0);
		}
	}

	@Test
	void testSkippedDuplicateKey() throws IOException {
		ResourceStorage storage = parse(simpleEntry(8, 0, 0, -1), simpleEntry(3, 0, 0, 0));
		assertThat(storage.getResources()).singleElement().satisfies(entry -> {
			assertThat(entry.getId()).isEqualTo(0x7f010000);
			assertThat(entry.getKeyName()).isEqualTo("flag");
			assertBool(entry.getSimpleValue(), -1);
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 3, 8, 12 })
	void testMapValueSize(int size) throws IOException {
		ByteBuffer entry = ByteBuffer.allocate(16 + 2 * 12).order(ByteOrder.LITTLE_ENDIAN);
		entry.putShort((short) 16);
		entry.putShort((short) FLAG_COMPLEX);
		entry.putInt(0); // key
		entry.putInt(0); // parent
		entry.putInt(2); // map count
		entry.putInt(0x01000000);
		putValue(entry, size, 0, -1);
		entry.putInt(0x01000001);
		putValue(entry, 8, 0, 0);

		ResourceStorage storage = parse(entry.array());
		assertThat(storage.getResources()).singleElement().satisfies(resource -> {
			List<RawNamedValue> values = resource.getNamedValues();
			assertThat(values).extracting(RawNamedValue::getNameRef).containsExactly(0x01000000, 0x01000001);
			assertBool(values.get(0).getRawValue(), -1);
			assertBool(values.get(1).getRawValue(), 0);
		});
	}

	private static void assertBool(RawValue value, int data) {
		assertThat(value.getDataType()).isEqualTo(TYPE_INT_BOOLEAN);
		assertThat(value.getData()).isEqualTo(data);
	}

	private static ResourceStorage parse(byte[]... entries) throws IOException {
		ResTableBinaryParser parser = new ResTableBinaryParser(new RootNode(new JadxArgs()));
		parser.decode(new ByteArrayInputStream(table(entries)));
		return parser.getResStorage();
	}

	private static byte[] table(byte[]... entries) {
		byte[] globalStrings = stringPool();
		byte[] typeStrings = stringPool("bool");
		byte[] keyStrings = stringPool("flag", "other");
		ByteBuffer typeSpec = chunk(RES_TABLE_TYPE_SPEC_TYPE, 16, 16 + entries.length * 4);
		typeSpec.putInt(1); // type id and reserved bytes
		typeSpec.putInt(entries.length);
		for (int i = 0; i < entries.length; i++) {
			typeSpec.putInt(0); // configuration flags
		}

		int typeHeaderSize = 20 + 64;
		int entriesStart = typeHeaderSize + entries.length * 4;
		int entriesSize = Arrays.stream(entries).mapToInt(entry -> entry.length).sum();
		ByteBuffer type = chunk(RES_TABLE_TYPE_TYPE, typeHeaderSize, entriesStart + entriesSize);
		type.putInt(1); // type id, flags and reserved bytes
		type.putInt(entries.length);
		type.putInt(entriesStart);
		type.putInt(64); // config size, followed by zero-filled config fields
		type.position(typeHeaderSize);
		int offset = 0;
		for (byte[] entry : entries) {
			type.putInt(offset);
			offset += entry.length;
		}
		for (byte[] entry : entries) {
			type.put(entry);
		}

		int packageHeaderSize = 288;
		int packageSize = packageHeaderSize + typeStrings.length + keyStrings.length + typeSpec.capacity() + type.capacity();
		ByteBuffer pkg = chunk(RES_TABLE_PACKAGE_TYPE, packageHeaderSize, packageSize);
		pkg.putInt(0x7f);
		pkg.put("com.example".getBytes(StandardCharsets.UTF_16LE));
		pkg.position(268); // end of fixed 128-character package name
		pkg.putInt(packageHeaderSize); // type strings offset
		pkg.putInt(0); // last public type
		pkg.putInt(packageHeaderSize + typeStrings.length); // key strings offset
		pkg.putInt(0); // last public key
		pkg.putInt(0); // type id offset
		pkg.put(typeStrings).put(keyStrings).put(typeSpec.array()).put(type.array());

		return chunk(RES_TABLE_TYPE, 12, 12 + globalStrings.length + packageSize)
				.putInt(1).put(globalStrings).put(pkg.array()).array();
	}

	private static byte[] simpleEntry(int size, int res0, int key, int data) {
		ByteBuffer entry = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
		entry.putShort((short) 8);
		entry.putShort((short) 0); // flags
		entry.putInt(key);
		putValue(entry, size, res0, data);
		return entry.array();
	}

	private static void putValue(ByteBuffer buf, int size, int res0, int data) {
		buf.putShort((short) size);
		buf.put((byte) res0);
		buf.put((byte) TYPE_INT_BOOLEAN);
		buf.putInt(data);
	}

	private static byte[] stringPool(String... strings) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		int[] offsets = new int[strings.length];
		for (int i = 0; i < strings.length; i++) {
			offsets[i] = data.size();
			byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_8);
			data.write(strings[i].length());
			data.write(bytes.length);
			data.writeBytes(bytes);
			data.write(0);
		}
		while (data.size() % 4 != 0) {
			data.write(0);
		}
		int stringsStart = 28 + offsets.length * 4;
		ByteBuffer buf = chunk(RES_STRING_POOL_TYPE, 28, stringsStart + data.size());
		buf.putInt(strings.length);
		buf.putInt(0); // styles count
		buf.putInt(UTF8_FLAG);
		buf.putInt(stringsStart);
		buf.putInt(0); // styles start
		for (int offset : offsets) {
			buf.putInt(offset);
		}
		return buf.put(data.toByteArray()).array();
	}

	private static ByteBuffer chunk(int type, int headerSize, int size) {
		ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) type).putShort((short) headerSize).putInt(size);
		return buf;
	}
}
