package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.android.aapt.ConfigurationOuterClass.Configuration;
import com.android.aapt.Resources.Array;
import com.android.aapt.Resources.Attribute;
import com.android.aapt.Resources.CompoundValue;
import com.android.aapt.Resources.ConfigValue;
import com.android.aapt.Resources.Entry;
import com.android.aapt.Resources.Item;
import com.android.aapt.Resources.Package;
import com.android.aapt.Resources.Plural;
import com.android.aapt.Resources.Primitive;
import com.android.aapt.Resources.ResourceTable;
import com.android.aapt.Resources.Style;
import com.android.aapt.Resources.Styleable;
import com.android.aapt.Resources.Type;
import com.android.aapt.Resources.Value;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.entry.EntryConfig;
import jadx.core.xmlgen.entry.ProtoValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResProtoParser implements IResParser {
	private final RootNode root;
	private final ResourceStorage resStorage = new ResourceStorage();

	public ResProtoParser(RootNode root) {
		this.root = root;
	}

	public ResContainer decodeFiles(InputStream inputStream) throws IOException {
		decode(inputStream);
		ValuesParser vp = new ValuesParser(new String[0], resStorage.getResourcesNames());
		ResXmlGen resGen = new ResXmlGen(resStorage, vp);
		ICodeInfo content = XmlGenUtils.makeXmlDump(root.makeCodeWriter(), resStorage);
		List<ResContainer> xmlFiles = resGen.makeResourcesXml();
		return ResContainer.resourceTable("res", xmlFiles, content);
	}

	@Override
	public void decode(InputStream inputStream) throws IOException {
		ResourceTable table = ResourceTable.parseFrom(FileUtils.streamToByteArray(inputStream));
		for (Package p : table.getPackageList()) {
			parse(p);
		}
		resStorage.finish();
	}

	private void parse(Package p) {
		String name = p.getPackageName();
		resStorage.setAppPackage(name);
		parse(name, p.getTypeList());
	}

	private void parse(String packageName, List<Type> types) {
		for (Type type : types) {
			String typeName = type.getName();
			for (Entry entry : type.getEntryList()) {
				int id = entry.getEntryId().getId();
				String entryName = entry.getName();
				for (ConfigValue configValue : entry.getConfigValueList()) {
					String config = parse(configValue.getConfig());
					ResourceEntry resEntry = new ResourceEntry(id, packageName, typeName, entryName, config);
					resStorage.add(resEntry);

					ProtoValue protoValue;
					if (configValue.getValue().getValueCase() == Value.ValueCase.ITEM) {
						protoValue = new ProtoValue(parse(configValue.getValue().getItem()));
					} else {
						protoValue = parse(configValue.getValue().getCompoundValue());
					}
					resEntry.setProtoValue(protoValue);
				}
			}
		}
	}

	private ProtoValue parse(Style s) {
		List<ProtoValue> namedValues = new ArrayList<>(s.getEntryCount());
		String parent = s.getParent().getName();
		if (parent.isEmpty()) {
			parent = null;
		} else {
			parent = '@' + parent;
		}
		for (int i = 0; i < s.getEntryCount(); i++) {
			Style.Entry entry = s.getEntry(i);
			String name = entry.getKey().getName();
			String value = parse(entry.getItem());
			namedValues.add(new ProtoValue(value).setName(name));
		}
		return new ProtoValue().setNamedValues(namedValues).setParent(parent);
	}

	private ProtoValue parse(Styleable s) {
		List<ProtoValue> namedValues = new ArrayList<>(s.getEntryCount());
		for (int i = 0; i < s.getEntryCount(); i++) {
			Styleable.Entry e = s.getEntry(i);
			namedValues.add(new ProtoValue('@' + e.getAttr().getName()));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	private ProtoValue parse(Array a) {
		List<ProtoValue> namedValues = new ArrayList<>(a.getElementCount());
		for (int i = 0; i < a.getElementCount(); i++) {
			Array.Element e = a.getElement(i);
			String value = parse(e.getItem());
			namedValues.add(new ProtoValue(value));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	private ProtoValue parse(Attribute a) {
		String format = XmlGenUtils.getAttrTypeAsString(a.getFormatFlags());
		List<ProtoValue> namedValues = new ArrayList<>(a.getSymbolCount());
		for (int i = 0; i < a.getSymbolCount(); i++) {
			Attribute.Symbol s = a.getSymbol(i);
			int type = s.getType();
			String name = s.getName().getName();
			String value = String.valueOf(s.getValue());
			namedValues.add(new ProtoValue(value).setName(name).setType(type));
		}
		return new ProtoValue(format).setNamedValues(namedValues);
	}

	private ProtoValue parse(Plural p) {
		List<ProtoValue> namedValues = new ArrayList<>(p.getEntryCount());
		for (int i = 0; i < p.getEntryCount(); i++) {
			Plural.Entry e = p.getEntry(i);
			String name = e.getArity().name();
			String value = parse(e.getItem());
			namedValues.add(new ProtoValue(value).setName(name));
		}
		return new ProtoValue().setNamedValues(namedValues);
	}

	private ProtoValue parse(CompoundValue c) {
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

	private String parse(Configuration c) {
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
		int size = c.getSerializedSize();
		byte uiMode = (byte) (c.getUiModeNightValue() | c.getUiModeTypeValue());

		c.getScreenLayoutSize(); // unknown field
		c.getProduct(); // unknown field

		return new EntryConfig(mcc, mnc, language, new char[] { '\00' },
				orientation, touchscreen, density, keyboard, navigation,
				inputFlags, screenWidth, screenHeight, sdkVersion,
				screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
				screenHeightDp, new char[] { '\00' }, new char[] { '\00' }, screenLayout2,
				colorMode, false, size).getQualifiers();
	}

	private String parse(Item i) {
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
			Primitive prim = i.getPrim();
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
			return '@' + i.getRef().getName();
		}
		if (i.hasFile()) {
			return i.getFile().getPath();
		}
		return "";
	}

	@Override
	public ResourceStorage getResStorage() {
		return resStorage;
	}

	@Override
	public String[] getStrings() {
		return new String[0];
	}
}
