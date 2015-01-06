package jadx.core.xmlgen.entry;

import jadx.core.xmlgen.ParserConstants;
import jadx.core.xmlgen.ResourceStorage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValuesParser extends ParserConstants {
	private static final Logger LOG = LoggerFactory.getLogger(ValuesParser.class);

	private final String[] strings;
	private final ResourceStorage resStorage;

	public ValuesParser(String[] strings, ResourceStorage resourceStorage) {
		this.strings = strings;
		this.resStorage = resourceStorage;
	}

	public String getValueString(ResourceEntry ri) {
		RawValue simpleValue = ri.getSimpleValue();
		if (simpleValue != null) {
			return decodeValue(simpleValue);
		}
		List<RawNamedValue> namedValues = ri.getNamedValues();
		List<String> strList = new ArrayList<String>(namedValues.size());
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

	public String decodeValue(RawValue value) {
		int dataType = value.getDataType();
		int data = value.getData();
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
				return Float.toString(Float.intBitsToFloat(data));

			case TYPE_INT_COLOR_ARGB8:
				return String.format("#%08x", data);
			case TYPE_INT_COLOR_RGB8:
				return String.format("#%06x", data & 0xFFFFFF);
			case TYPE_INT_COLOR_ARGB4:
				return String.format("#%04x", data & 0xFFFF);
			case TYPE_INT_COLOR_RGB4:
				return String.format("#%03x", data & 0xFFF);

			case TYPE_REFERENCE: {
				ResourceEntry ri = resStorage.getByRef(data);
				if (ri == null) {
					return "?unknown_ref: " + Integer.toHexString(data);
				}
				return ri.formatAsRef();
			}

			case TYPE_ATTRIBUTE: {
				ResourceEntry ri = resStorage.getByRef(data);
				if (ri == null) {
					return "?unknown_ref: " + Integer.toHexString(data);
				}
				return ri.formatAsAttribute();
			}

			case TYPE_DIMENSION:
				return decodeComplex(data, false);
			case TYPE_FRACTION:
				return decodeComplex(data, true);

			default:
				LOG.warn("Unknown data type: 0x" + Integer.toHexString(dataType) + " " + data);
				return "  ?0x" + Integer.toHexString(dataType) + " " + data;
		}
	}

	private String decodeNameRef(int nameRef) {
		int ref = nameRef;
		if (isResInternalId(nameRef)) {
			ref = nameRef & ATTR_TYPE_ANY;
			if (ref == 0) {
				return null;
			}
		}
		ResourceEntry ri = resStorage.getByRef(ref);
		if (ri != null) {
			return ri.getTypeName() + "." + ri.getKeyName();
		}
		return "?0x" + Integer.toHexString(nameRef);
	}

	private String decodeComplex(int data, boolean isFraction) {
		double value = (data & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
				* RADIX_MULTS[(data >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
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
		if (value == Math.ceil(value)) {
			return Integer.toString((int) value);
		} else {
			// remove trailing zeroes
			NumberFormat f = NumberFormat.getInstance();
			f.setMaximumFractionDigits(4);
			f.setMinimumIntegerDigits(1);
			return f.format(value);
		}
	}
}
