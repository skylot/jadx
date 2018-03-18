package jadx.core.xmlgen.entry;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.xmlgen.ParserConstants;
import jadx.core.xmlgen.ResTableParser;

public class ValuesParser extends ParserConstants {
	private static final Logger LOG = LoggerFactory.getLogger(ValuesParser.class);

	private static String[] androidStrings;
	private static Map<Integer, String> androidResMap;

	private final String[] strings;
	private final Map<Integer, String> resMap;

	public ValuesParser(String[] strings, Map<Integer, String> resMap) {
		this.strings = strings;
		this.resMap = resMap;

		if (androidStrings == null && androidResMap == null) {
			try {
				decodeAndroid();
			} catch (Exception e) {
				LOG.error("Failed to decode Android Resource file", e);
			}
		}
	}

	private static void decodeAndroid() throws IOException {
		InputStream inputStream = new BufferedInputStream(ValuesParser.class.getResourceAsStream("/resources.arsc"));
		ResTableParser androidParser = new ResTableParser();
		androidParser.decode(inputStream);
		androidStrings = androidParser.getStrings();
		androidResMap = androidParser.getResStorage().getResourcesNames();
	}

	@Nullable
	public String getValueString(ResourceEntry ri) {
		RawValue simpleValue = ri.getSimpleValue();
		if (simpleValue != null) {
			return decodeValue(simpleValue);
		}
		List<RawNamedValue> namedValues = ri.getNamedValues();
		List<String> strList = new ArrayList<>(namedValues.size());
		for (RawNamedValue value : namedValues) {
			String nameStr = decodeNameRef(value.getNameRef());
			String valueStr = decodeValue(value.getRawValue());
			if (nameStr == null) {
				strList.add(valueStr);
			} else {
				strList.add(nameStr + "=" + valueStr);
			}
		}
		return strList.toString();
	}

	@Nullable
	public String decodeValue(RawValue value) {
		int dataType = value.getDataType();
		int data = value.getData();
		return decodeValue(dataType, data);
	}

	@Nullable
	public String decodeValue(int dataType, int data) {
		switch (dataType) {
			case TYPE_NULL:
				return null;
			case TYPE_STRING:
				return strings[data];
			case TYPE_INT_DEC:
				return Integer.toString(data);
			case TYPE_INT_HEX:
				return Integer.toHexString(data);
			case TYPE_INT_BOOLEAN:
				return data == 0 ? "false" : "true";
			case TYPE_FLOAT:
				return floatToString(Float.intBitsToFloat(data));

			case TYPE_INT_COLOR_ARGB8:
				return String.format("#%08x", data);
			case TYPE_INT_COLOR_RGB8:
				return String.format("#%06x", data & 0xFFFFFF);
			case TYPE_INT_COLOR_ARGB4:
				return String.format("#%04x", data & 0xFFFF);
			case TYPE_INT_COLOR_RGB4:
				return String.format("#%03x", data & 0xFFF);

			case TYPE_REFERENCE: {
				String ri = resMap.get(data);
				if (ri == null) {
					String androidRi = androidResMap.get(data);
					if (androidRi != null) {
						return "@android:" + androidRi;
					}
					if (data == 0) {
						return "0";
					}
					return "?unknown_ref: " + Integer.toHexString(data);
				}
				return "@" + ri;
			}

			case TYPE_ATTRIBUTE: {
				String ri = resMap.get(data);
				if (ri == null) {
					String androidRi = androidResMap.get(data);
					if (androidRi != null) {
						return "?android:" + androidRi;
					}
					return "?unknown_attr_ref: " + Integer.toHexString(data);
				}
				return "?" + ri;
			}

			case TYPE_DIMENSION:
				return decodeComplex(data, false);
			case TYPE_FRACTION:
				return decodeComplex(data, true);

			default:
				LOG.warn("Unknown data type: 0x{} {}", Integer.toHexString(dataType), data);
				return "  ?0x" + Integer.toHexString(dataType) + " " + data;
		}
	}

	public String decodeNameRef(int nameRef) {
		int ref = nameRef;
		if (isResInternalId(nameRef)) {
			ref = nameRef & ATTR_TYPE_ANY;
			if (ref == 0) {
				return null;
			}
		}
		String ri = resMap.get(ref);
		if (ri != null) {
			return ri.replace('/', '.');
		} else {
			String androidRi = androidResMap.get(ref);
			if (androidRi != null) {
				return "android:" + androidRi.replace('/', '.');
			}
		}
		return "?0x" + Integer.toHexString(nameRef);
	}

	private String decodeComplex(int data, boolean isFraction) {
		double value = (data & COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT)
				* RADIX_MULTS[data >> COMPLEX_RADIX_SHIFT & COMPLEX_RADIX_MASK];
		int unitType = data & COMPLEX_UNIT_MASK;
		String unit;
		if (isFraction) {
			value *= 100;
			switch (unitType) {
				case COMPLEX_UNIT_FRACTION:
					unit = "%";
					break;
				case COMPLEX_UNIT_FRACTION_PARENT:
					unit = "%p";
					break;

				default:
					unit = "?f" + Integer.toHexString(unitType);
			}
		} else {
			switch (unitType) {
				case COMPLEX_UNIT_PX:
					unit = "px";
					break;
				case COMPLEX_UNIT_DIP:
					unit = "dp";
					break;
				case COMPLEX_UNIT_SP:
					unit = "sp";
					break;
				case COMPLEX_UNIT_PT:
					unit = "pt";
					break;
				case COMPLEX_UNIT_IN:
					unit = "in";
					break;
				case COMPLEX_UNIT_MM:
					unit = "mm";
					break;

				default:
					unit = "?d" + Integer.toHexString(unitType);
			}
		}
		return doubleToString(value) + unit;
	}

	private static String doubleToString(double value) {
		if (Double.compare(value, Math.floor(value)) == 0
				&& !Double.isInfinite(value)) {
			return Integer.toString((int) value);
		}
		// remove trailing zeroes
		NumberFormat f = NumberFormat.getInstance(Locale.ROOT);
		f.setMaximumFractionDigits(4);
		f.setMinimumIntegerDigits(1);
		return f.format(value);
	}

	private static String floatToString(float value) {
		return doubleToString((double) value);
	}

	public static Map<Integer, String> getAndroidResMap() {
		return androidResMap;
	}
}
