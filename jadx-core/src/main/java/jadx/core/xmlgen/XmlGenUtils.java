package jadx.core.xmlgen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class XmlGenUtils {
	private XmlGenUtils() {
	}

	public static byte[] readData(InputStream i) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int read;
		while ((read = i.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, read);
		}
		return buffer.toByteArray();
	}

	public static ICodeInfo makeXmlDump(ICodeWriter writer, ResourceStorage resStorage) {
		writer.startLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.startLine("<resources>");
		writer.incIndent();

		Set<String> addedValues = new HashSet<>();
		for (ResourceEntry ri : resStorage.getResources()) {
			if (addedValues.add(ri.getTypeName() + '.' + ri.getKeyName())) {
				String format = String.format("<public type=\"%s\" name=\"%s\" id=\"0x%08x\" />",
						ri.getTypeName(), ri.getKeyName(), ri.getId());
				writer.startLine(format);
			}
		}
		writer.decIndent();
		writer.startLine("</resources>");
		return writer.finish();
	}

	public static String decodeComplex(int data, boolean isFraction) {
		double value = (data & ParserConstants.COMPLEX_MANTISSA_MASK << ParserConstants.COMPLEX_MANTISSA_SHIFT)
				* ParserConstants.RADIX_MULTS[data >> ParserConstants.COMPLEX_RADIX_SHIFT & ParserConstants.COMPLEX_RADIX_MASK];
		int unitType = data & ParserConstants.COMPLEX_UNIT_MASK;
		String unit;
		if (isFraction) {
			value *= 100;
			switch (unitType) {
				case ParserConstants.COMPLEX_UNIT_FRACTION:
					unit = "%";
					break;
				case ParserConstants.COMPLEX_UNIT_FRACTION_PARENT:
					unit = "%p";
					break;

				default:
					unit = "?f" + Integer.toHexString(unitType);
			}
		} else {
			switch (unitType) {
				case ParserConstants.COMPLEX_UNIT_PX:
					unit = "px";
					break;
				case ParserConstants.COMPLEX_UNIT_DIP:
					unit = "dp";
					break;
				case ParserConstants.COMPLEX_UNIT_SP:
					unit = "sp";
					break;
				case ParserConstants.COMPLEX_UNIT_PT:
					unit = "pt";
					break;
				case ParserConstants.COMPLEX_UNIT_IN:
					unit = "in";
					break;
				case ParserConstants.COMPLEX_UNIT_MM:
					unit = "mm";
					break;

				default:
					unit = "?d" + Integer.toHexString(unitType);
			}
		}
		return doubleToString(value) + unit;
	}

	public static String doubleToString(double value) {
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

	public static String floatToString(float value) {
		return doubleToString(value);
	}

	public static String getAttrTypeAsString(int type) {
		String s = "";
		if ((type & ValuesParser.ATTR_TYPE_REFERENCE) != 0) {
			s += "|reference";
		}
		if ((type & ValuesParser.ATTR_TYPE_STRING) != 0) {
			s += "|string";
		}
		if ((type & ValuesParser.ATTR_TYPE_INTEGER) != 0) {
			s += "|integer";
		}
		if ((type & ValuesParser.ATTR_TYPE_BOOLEAN) != 0) {
			s += "|boolean";
		}
		if ((type & ValuesParser.ATTR_TYPE_COLOR) != 0) {
			s += "|color";
		}
		if ((type & ValuesParser.ATTR_TYPE_FLOAT) != 0) {
			s += "|float";
		}
		if ((type & ValuesParser.ATTR_TYPE_DIMENSION) != 0) {
			s += "|dimension";
		}
		if ((type & ValuesParser.ATTR_TYPE_FRACTION) != 0) {
			s += "|fraction";
		}
		if (s.isEmpty()) {
			return null;
		}
		return s.substring(1);
	}
}
