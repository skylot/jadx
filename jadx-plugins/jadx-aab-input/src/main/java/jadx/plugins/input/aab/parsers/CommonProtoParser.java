package jadx.plugins.input.aab.parsers;

import java.util.ArrayList;
import java.util.List;

import com.android.aapt.ConfigurationOuterClass;
import com.android.aapt.Resources;

import jadx.core.xmlgen.ParserConstants;
import jadx.core.xmlgen.XmlGenUtils;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.ProtoValue;

public class CommonProtoParser extends ParserConstants {
	protected ProtoValue parse(Resources.Style s) {
		List<ProtoValue> namedValues = new ArrayList<>(s.getEntryCount());
		String parent = s.getParent().getName();
		if (parent.isEmpty()) {
			parent = null;
		} else {
			parent = '@' + parent;
		}
		for (int i = 0; i < s.getEntryCount(); i++) {
			Resources.Style.Entry entry = s.getEntry(i);
			String name = entry.getKey().getName();
			String value = parse(entry.getItem());
			namedValues.add(new ProtoValue(value).setName(name));
		}
		return new ProtoValue().setNamedValues(namedValues).setParent(parent);
	}

	protected ProtoValue parse(Resources.Styleable s) {
		List<ProtoValue> namedValues = new ArrayList<>(s.getEntryCount());
		for (int i = 0; i < s.getEntryCount(); i++) {
			Resources.Styleable.Entry e = s.getEntry(i);
			namedValues.add(new ProtoValue('@' + e.getAttr().getName()));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	protected ProtoValue parse(Resources.Array a) {
		List<ProtoValue> namedValues = new ArrayList<>(a.getElementCount());
		for (int i = 0; i < a.getElementCount(); i++) {
			Resources.Array.Element e = a.getElement(i);
			String value = parse(e.getItem());
			namedValues.add(new ProtoValue(value));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	protected ProtoValue parse(Resources.Attribute a) {
		String format = XmlGenUtils.getAttrTypeAsString(a.getFormatFlags());
		List<ProtoValue> namedValues = new ArrayList<>(a.getSymbolCount());
		for (int i = 0; i < a.getSymbolCount(); i++) {
			Resources.Attribute.Symbol s = a.getSymbol(i);
			int type = s.getType();
			String name = s.getName().getName();
			String value = String.valueOf(s.getValue());
			namedValues.add(new ProtoValue(value).setName(name).setType(type));
		}
		return new ProtoValue(format).setNamedValues(namedValues);
	}

	protected ProtoValue parse(Resources.Plural p) {
		List<ProtoValue> namedValues = new ArrayList<>(p.getEntryCount());
		for (int i = 0; i < p.getEntryCount(); i++) {
			Resources.Plural.Entry e = p.getEntry(i);
			String name = e.getArity().name();
			String value = parse(e.getItem());
			namedValues.add(new ProtoValue(value).setName(name));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	protected ProtoValue parse(Resources.CompoundValue c) {
		switch (c.getValueCase()) {
			case STYLE:
				return parse(c.getStyle());
			case STYLEABLE:
				return parse(c.getStyleable());
			case ARRAY:
				return parse(c.getArray());
			case ATTR:
				return parse(c.getAttr());
			case PLURAL:
				return parse(c.getPlural());
			default:
				return new ProtoValue("Unresolved value");
		}
	}

	protected String parse(ConfigurationOuterClass.Configuration c) {
		char[] language = c.getLocale().toCharArray();
		if (language.length == 0) {
			language = new char[] { '\00' };
		}
		short mcc = (short) c.getMcc();
		short mnc = (short) c.getMnc();
		byte orientation = (byte) c.getOrientationValue();
		short screenWidth = (short) c.getScreenWidth();
		short screenHeight = (short) c.getScreenHeight();
		short screenWidthDp = (short) c.getScreenWidthDp();
		short screenHeightDp = (short) c.getScreenHeightDp();
		short smallestScreenWidthDp = (short) c.getSmallestScreenWidthDp();
		short sdkVersion = (short) c.getSdkVersion();
		byte keyboard = (byte) c.getKeyboardValue();
		byte touchscreen = (byte) c.getTouchscreenValue();
		int density = c.getDensity();
		byte screenLayout = (byte) c.getScreenLayoutLongValue();
		byte colorMode = (byte) (c.getHdrValue() | c.getWideColorGamutValue());
		byte screenLayout2 = (byte) (c.getLayoutDirectionValue() | c.getScreenRoundValue());
		byte navigation = (byte) c.getNavigationValue();
		byte inputFlags = (byte) (c.getKeysHiddenValue() | c.getNavHiddenValue());
		byte grammaticalInflection = (byte) c.getGrammaticalGenderValue();
		int size = c.getSerializedSize();
		byte uiMode = (byte) (c.getUiModeNightValue() | c.getUiModeTypeValue());

		c.getScreenLayoutSize(); // unknown field
		c.getProduct(); // unknown field

		return new EntryConfig(mcc, mnc, language, new char[] { '\00' },
				orientation, touchscreen, density, keyboard, navigation,
				inputFlags, grammaticalInflection, screenWidth, screenHeight, sdkVersion,
				screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
				screenHeightDp, new char[] { '\00' }, new char[] { '\00' }, screenLayout2,
				colorMode, false, size).getQualifiers();
	}

	protected String parse(Resources.Item i) {
		if (i.hasRawStr()) {
			return i.getRawStr().getValue();
		}
		if (i.hasStr()) {
			return i.getStr().getValue();
		}
		if (i.hasStyledStr()) {
			return i.getStyledStr().getValue();
		}
		if (i.hasPrim()) {
			Resources.Primitive prim = i.getPrim();
			switch (prim.getOneofValueCase()) {
				case NULL_VALUE:
					return null;
				case INT_DECIMAL_VALUE:
					return String.valueOf(prim.getIntDecimalValue());
				case INT_HEXADECIMAL_VALUE:
					return Integer.toHexString(prim.getIntHexadecimalValue());
				case BOOLEAN_VALUE:
					return String.valueOf(prim.getBooleanValue());
				case FLOAT_VALUE:
					return String.valueOf(prim.getFloatValue());
				case COLOR_ARGB4_VALUE:
					return String.format("#%04x", prim.getColorArgb4Value());
				case COLOR_ARGB8_VALUE:
					return String.format("#%08x", prim.getColorArgb8Value());
				case COLOR_RGB4_VALUE:
					return String.format("#%03x", prim.getColorRgb4Value());
				case COLOR_RGB8_VALUE:
					return String.format("#%06x", prim.getColorRgb8Value());
				case DIMENSION_VALUE:
					return XmlGenUtils.decodeComplex(prim.getDimensionValue(), false);
				case FRACTION_VALUE:
					return XmlGenUtils.decodeComplex(prim.getDimensionValue(), true);
				case EMPTY_VALUE:
				default:
					return "";
			}
		}
		if (i.hasRef()) {
			Resources.Reference ref = i.getRef();
			String value = ref.getName();
			if (value.isEmpty()) {
				value = "id/" + ref.getId();
			}
			return '@' + value;
		}
		if (i.hasFile()) {
			return i.getFile().getPath();
		}
		return "";
	}
}
