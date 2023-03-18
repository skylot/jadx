package jadx.core.xmlgen;

import java.util.HashMap;
import java.util.Map;

public class ParserConstants {

	protected ParserConstants() {
	}

	protected static final String ANDROID_NS_URL = "http://schemas.android.com/apk/res/android";
	protected static final String ANDROID_NS_VALUE = "android";

	/**
	 * Chunk types
	 */
	protected static final int RES_NULL_TYPE = 0x0000;
	protected static final int RES_STRING_POOL_TYPE = 0x0001;
	protected static final int RES_TABLE_TYPE = 0x0002;

	protected static final int RES_XML_TYPE = 0x0003;
	protected static final int RES_XML_FIRST_CHUNK_TYPE = 0x0100;
	protected static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
	protected static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
	protected static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
	protected static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
	protected static final int RES_XML_CDATA_TYPE = 0x0104;
	protected static final int RES_XML_LAST_CHUNK_TYPE = 0x017f;
	protected static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

	protected static final int RES_TABLE_PACKAGE_TYPE = 0x0200; // 512
	protected static final int RES_TABLE_TYPE_TYPE = 0x0201; // 513
	protected static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202; // 514
	protected static final int RES_TABLE_TYPE_LIBRARY = 0x0203; // 515
	protected static final int RES_TABLE_TYPE_OVERLAY = 0x0204; // 516
	protected static final int RES_TABLE_TYPE_OVERLAY_POLICY = 0x0205; // 517
	protected static final int RES_TABLE_TYPE_STAGED_ALIAS = 0x0206; // 518

	/**
	 * Type constants
	 */
	// Contains no data.
	protected static final int TYPE_NULL = 0x00;
	// The 'data' holds a ResTable_ref, a reference to another resource table entry.
	protected static final int TYPE_REFERENCE = 0x01;
	// The 'data' holds an attribute resource identifier.
	protected static final int TYPE_ATTRIBUTE = 0x02;
	// The 'data' holds an index into the containing resource table's global value string pool.
	protected static final int TYPE_STRING = 0x03;
	// The 'data' holds a single-precision floating point number.
	protected static final int TYPE_FLOAT = 0x04;
	// The 'data' holds a complex number encoding a dimension value, such as "100in".
	protected static final int TYPE_DIMENSION = 0x05;
	// The 'data' holds a complex number encoding a fraction of a container.
	protected static final int TYPE_FRACTION = 0x06;

	/**
	 * The 'data' holds a dynamic reference, a reference to another resource table entry.
	 * See https://github.com/skylot/jadx/issues/919
	 */
	protected static final int TYPE_DYNAMIC_REFERENCE = 0x07;

	/**
	 * According to the sources of apktool this type seem to be related to themes
	 * See https://github.com/skylot/jadx/issues/919
	 */
	protected static final int TYPE_DYNAMIC_ATTRIBUTE = 0x08;
	// Beginning of integer flavors...
	protected static final int TYPE_FIRST_INT = 0x10;
	// The 'data' is a raw integer value of the form n..n.
	protected static final int TYPE_INT_DEC = 0x10;
	// The 'data' is a raw integer value of the form 0xn..n.
	protected static final int TYPE_INT_HEX = 0x11;
	// The 'data' is either 0 or 1, for input "false" or "true" respectively.
	protected static final int TYPE_INT_BOOLEAN = 0x12;
	// Beginning of color integer flavors...
	protected static final int TYPE_FIRST_COLOR_INT = 0x1c;
	// The 'data' is a raw integer value of the form #aarrggbb.
	protected static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
	// The 'data' is a raw integer value of the form #rrggbb.
	protected static final int TYPE_INT_COLOR_RGB8 = 0x1d;
	// The 'data' is a raw integer value of the form #argb.
	protected static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
	// The 'data' is a raw integer value of the form #rgb.
	protected static final int TYPE_INT_COLOR_RGB4 = 0x1f;
	// ...end of integer flavors.
	protected static final int TYPE_LAST_COLOR_INT = 0x1f;
	// ...end of integer flavors.
	protected static final int TYPE_LAST_INT = 0x1f;

	// Where the unit type information is. This gives us 16 possible
	// types, as defined below.
	protected static final int COMPLEX_UNIT_SHIFT = 0;
	protected static final int COMPLEX_UNIT_MASK = 0xf;

	// TYPE_DIMENSION: Value is raw pixels.
	protected static final int COMPLEX_UNIT_PX = 0;
	// TYPE_DIMENSION: Value is Device Independent Pixels.
	protected static final int COMPLEX_UNIT_DIP = 1;
	// TYPE_DIMENSION: Value is a Scaled device independent Pixels.
	protected static final int COMPLEX_UNIT_SP = 2;
	// TYPE_DIMENSION: Value is in points.
	protected static final int COMPLEX_UNIT_PT = 3;
	// TYPE_DIMENSION: Value is in inches.
	protected static final int COMPLEX_UNIT_IN = 4;
	// TYPE_DIMENSION: Value is in millimeters.
	protected static final int COMPLEX_UNIT_MM = 5;

	// TYPE_FRACTION: A basic fraction of the overall size.
	protected static final int COMPLEX_UNIT_FRACTION = 0;
	// TYPE_FRACTION: A fraction of the parent size.
	protected static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

	// Where the radix information is, telling where the decimal place
	// appears in the mantissa. This give us 4 possible fixed point
	// representations as defined below.
	protected static final int COMPLEX_RADIX_SHIFT = 4;
	protected static final int COMPLEX_RADIX_MASK = 0x3;

	// The mantissa is an integral number -- i.e., 0xnnnnnn.0
	protected static final int COMPLEX_RADIX_23P0 = 0;
	// The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
	protected static final int COMPLEX_RADIX_16P7 = 1;
	// The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
	protected static final int COMPLEX_RADIX_8P15 = 2;
	// The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
	protected static final int COMPLEX_RADIX_0P23 = 3;

	// Where the actual value is. This gives us 23 bits of
	// precision. The top bit is the sign.
	protected static final int COMPLEX_MANTISSA_SHIFT = 8;
	protected static final int COMPLEX_MANTISSA_MASK = 0xffffff;

	protected static final double MANTISSA_MULT = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
	protected static final double[] RADIX_MULTS = new double[] {
			1.0f * MANTISSA_MULT,
			1.0f / (1 << 7) * MANTISSA_MULT,
			1.0f / (1 << 15) * MANTISSA_MULT,
			1.0f / (1 << 23) * MANTISSA_MULT
	};

	/**
	 * String pool flags
	 */
	protected static final int SORTED_FLAG = 1;
	protected static final int UTF8_FLAG = 1 << 8;

	protected static final int NO_ENTRY = 0xFFFFFFFF;

	/**
	 * ResTable_entry
	 */
	// If set, this is a complex entry, holding a set of name/value mappings.
	// It is followed by an array of ResTable_map structures.
	protected static final int FLAG_COMPLEX = 0x0001;
	// If set, this resource has been declared public, so libraries are allowed to reference it.
	protected static final int FLAG_PUBLIC = 0x0002;
	// If set, this is a weak resource and may be overriden by strong resources of the same name/type.
	// This is only useful during linking with other resource tables.
	protected static final int FLAG_WEAK = 0x0004;

	/**
	 * ResTable_map
	 */
	protected static final int ATTR_TYPE = makeResInternal(0);
	// For integral attributes, this is the minimum value it can hold.
	protected static final int ATTR_MIN = makeResInternal(1);
	// For integral attributes, this is the maximum value it can hold.
	protected static final int ATTR_MAX = makeResInternal(2);
	// Localization of this resource is can be encouraged or required with an aapt flag if this is set
	protected static final int ATTR_L10N = makeResInternal(3);

	// for plural support, see android.content.res.PluralRules#attrForQuantity(int)
	protected static final int ATTR_OTHER = makeResInternal(4);
	protected static final int ATTR_ZERO = makeResInternal(5);
	protected static final int ATTR_ONE = makeResInternal(6);
	protected static final int ATTR_TWO = makeResInternal(7);
	protected static final int ATTR_FEW = makeResInternal(8);
	protected static final int ATTR_MANY = makeResInternal(9);

	protected static final Map<Integer, String> PLURALS_MAP;

	static {
		PLURALS_MAP = new HashMap<>();
		PLURALS_MAP.put(ATTR_OTHER, "other");
		PLURALS_MAP.put(ATTR_ZERO, "zero");
		PLURALS_MAP.put(ATTR_ONE, "one");
		PLURALS_MAP.put(ATTR_TWO, "two");
		PLURALS_MAP.put(ATTR_FEW, "few");
		PLURALS_MAP.put(ATTR_MANY, "many");
	}

	private static int makeResInternal(int entry) {
		return 0x01000000 | entry & 0xFFFF;
	}

	protected static boolean isResInternalId(int resid) {
		return (resid & 0xFFFF0000) != 0 && (resid & 0xFF0000) == 0;
	}

	// Bit mask of allowed types, for use with ATTR_TYPE.
	protected static final int ATTR_TYPE_ANY = 0x0000FFFF;
	// Attribute holds a references to another resource.
	protected static final int ATTR_TYPE_REFERENCE = 1;
	// Attribute holds a generic string.
	protected static final int ATTR_TYPE_STRING = 1 << 1;
	// Attribute holds an integer value. ATTR_MIN and ATTR_MIN can
	// optionally specify a constrained range of possible integer values.
	protected static final int ATTR_TYPE_INTEGER = 1 << 2;
	// Attribute holds a boolean integer.
	protected static final int ATTR_TYPE_BOOLEAN = 1 << 3;
	// Attribute holds a color value.
	protected static final int ATTR_TYPE_COLOR = 1 << 4;
	// Attribute holds a floating point value.
	protected static final int ATTR_TYPE_FLOAT = 1 << 5;
	// Attribute holds a dimension value, such as "20px".
	protected static final int ATTR_TYPE_DIMENSION = 1 << 6;
	// Attribute holds a fraction value, such as "20%".
	protected static final int ATTR_TYPE_FRACTION = 1 << 7;
	// Attribute holds an enumeration. The enumeration values are
	// supplied as additional entries in the map.
	protected static final int ATTR_TYPE_ENUM = 1 << 16;
	// Attribute holds a bitmaks of flags. The flag bit values are
	// supplied as additional entries in the map.
	protected static final int ATTR_TYPE_FLAGS = 1 << 17;

	// Enum of localization modes, for use with ATTR_L10N
	protected static final int ATTR_L10N_NOT_REQUIRED = 0;
	protected static final int ATTR_L10N_SUGGESTED = 1;
}
