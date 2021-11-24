package jadx.core.xmlgen.entry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.android.TextResMapFile;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ParserConstants;
import jadx.core.xmlgen.XmlGenUtils;

public class ValuesParser extends ParserConstants {
	private static final Logger LOG = LoggerFactory.getLogger(ValuesParser.class);

	private static Map<Integer, String> androidResMap;

	private final String[] strings;
	private final Map<Integer, String> resMap;

	public ValuesParser(String[] strings, Map<Integer, String> resMap) {
		this.strings = strings;
		this.resMap = resMap;
		getAndroidResMap();
	}

	public static Map<Integer, String> getAndroidResMap() {
		if (androidResMap == null) {
			androidResMap = loadAndroidResMap();
		}
		return androidResMap;
	}

	private static Map<Integer, String> loadAndroidResMap() {
		try (InputStream is = ValuesParser.class.getResourceAsStream("/android/res-map.txt")) {
			return TextResMapFile.read(is);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load android resource file", e);
		}
	}

	@Nullable
	public String getSimpleValueString(ResourceEntry ri) {
		ProtoValue protoValue = ri.getProtoValue();
		if (protoValue != null) {
			return protoValue.getValue();
		}
		RawValue simpleValue = ri.getSimpleValue();
		if (simpleValue == null) {
			return null;
		}
		return decodeValue(simpleValue);
	}

	@Nullable
	public String getValueString(ResourceEntry ri) {
		ProtoValue protoValue = ri.getProtoValue();
		if (protoValue != null) {
			if (protoValue.getValue() != null) {
				return protoValue.getValue();
			}
			List<ProtoValue> values = protoValue.getNamedValues();
			List<String> strList = new ArrayList<>(values.size());
			for (ProtoValue value : values) {
				if (value.getName() == null) {
					strList.add(value.getValue());
				} else {
					strList.add(value.getName() + '=' + value.getValue());
				}
			}
			return strList.toString();
		}
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
				strList.add(nameStr + '=' + valueStr);
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
				return "0x" + Integer.toHexString(data);
			case TYPE_INT_BOOLEAN:
				return data == 0 ? "false" : "true";
			case TYPE_FLOAT:
				return XmlGenUtils.floatToString(Float.intBitsToFloat(data));
			case TYPE_INT_COLOR_ARGB8:
				return String.format("#%08x", data);
			case TYPE_INT_COLOR_RGB8:
				return String.format("#%06x", data & 0xFFFFFF);
			case TYPE_INT_COLOR_ARGB4:
				return String.format("#%04x", data & 0xFFFF);
			case TYPE_INT_COLOR_RGB4:
				return String.format("#%03x", data & 0xFFF);

			case TYPE_DYNAMIC_REFERENCE:
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
				return '@' + ri;
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
				return '?' + ri;
			}

			case TYPE_DIMENSION:
				return XmlGenUtils.decodeComplex(data, false);
			case TYPE_FRACTION:
				return XmlGenUtils.decodeComplex(data, true);
			case TYPE_DYNAMIC_ATTRIBUTE:
				LOG.warn("Data type TYPE_DYNAMIC_ATTRIBUTE not yet supported: {}", data);
				return "  TYPE_DYNAMIC_ATTRIBUTE: " + data;

			default:
				LOG.warn("Unknown data type: 0x{} {}", Integer.toHexString(dataType), data);
				return "  ?0x" + Integer.toHexString(dataType) + ' ' + data;
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
}
