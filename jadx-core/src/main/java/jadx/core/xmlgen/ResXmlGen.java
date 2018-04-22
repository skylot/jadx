package jadx.core.xmlgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.core.codegen.CodeWriter;
import jadx.core.utils.StringUtils;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResXmlGen {

	private static final Set<String> SKIP_RES_TYPES = new HashSet<>(Arrays.asList(
			"layout",
			"mipmap",
			"id"
	));

	private final ResourceStorage resStorage;
	private final ValuesParser vp;

	public ResXmlGen(ResourceStorage resStorage, ValuesParser vp) {
		this.resStorage = resStorage;
		this.vp = vp;
	}

	public List<ResContainer> makeResourcesXml() {
		Map<String, CodeWriter> contMap = new HashMap<>();
		for (ResourceEntry ri : resStorage.getResources()) {
			if (SKIP_RES_TYPES.contains(ri.getTypeName())) {
				continue;
			}
			String fn = getFileName(ri);
			CodeWriter cw = contMap.get(fn);
			if (cw == null) {
				cw = new CodeWriter();
				cw.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				cw.startLine("<resources>");
				cw.incIndent();
				contMap.put(fn, cw);
			}
			addValue(cw, ri);
		}

		List<ResContainer> files = new ArrayList<>(contMap.size());
		for (Map.Entry<String, CodeWriter> entry : contMap.entrySet()) {
			String fileName = entry.getKey();
			CodeWriter content = entry.getValue();

			content.decIndent();
			content.startLine("</resources>");
			content.finish();
			files.add(ResContainer.singleFile(fileName, content));
		}
		Collections.sort(files);
		return files;
	}

	private void addValue(CodeWriter cw, ResourceEntry ri) {
		if (ri.getSimpleValue() != null) {
			String valueStr = vp.decodeValue(ri.getSimpleValue());
			addSimpleValue(cw, ri.getTypeName(), ri.getTypeName(), "name", ri.getKeyName(), valueStr);
		} else {
			cw.startLine();
			cw.add('<').add(ri.getTypeName()).add(' ');
			String itemTag = "item";
			if (ri.getTypeName().equals("attr") && !ri.getNamedValues().isEmpty()) {
				cw.add("name=\"").add(ri.getKeyName());
				int type = ri.getNamedValues().get(0).getRawValue().getData();
				if ((type & ValuesParser.ATTR_TYPE_ENUM) != 0) {
					itemTag = "enum";
				} else if ((type & ValuesParser.ATTR_TYPE_FLAGS) != 0) {
					itemTag = "flag";
				}
				String formatValue = getTypeAsString(type);
				if (formatValue != null) {
					cw.add("\" format=\"").add(formatValue);
				}
				cw.add("\">");
			} else {
				cw.add("name=\"").add(ri.getKeyName()).add("\">");
			}
			cw.incIndent();
			for (RawNamedValue value : ri.getNamedValues()) {
				addItem(cw, itemTag, ri.getTypeName(), value);
			}
			cw.decIndent();
			cw.startLine().add("</").add(ri.getTypeName()).add('>');
		}
	}

	private String getTypeAsString(int type) {
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

	private void addItem(CodeWriter cw, String itemTag, String typeName, RawNamedValue value) {
		String nameStr = vp.decodeNameRef(value.getNameRef());
		String valueStr = vp.decodeValue(value.getRawValue());
		if (!typeName.equals("attr")) {
			if (valueStr == null || valueStr.equals("0")) {
				valueStr = "@null";
			}
			if (nameStr != null) {
				try {
					int intVal = Integer.parseInt(valueStr);
					String newVal = ManifestAttributes.getInstance().decode(nameStr.replace("android:attr.", ""), intVal);
					if (newVal != null) {
						valueStr = newVal;
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		if (typeName.equals("attr")) {
			if (nameStr != null) {
				addSimpleValue(cw, typeName, itemTag, nameStr, valueStr, "");
			}
		} else if (typeName.equals("style")) {
			if (nameStr != null) {
				addSimpleValue(cw, typeName, itemTag, nameStr, "", valueStr);
			}
		} else {
			addSimpleValue(cw, typeName, itemTag, null, null, valueStr);
		}
	}

	private void addSimpleValue(CodeWriter cw, String typeName, String itemTag, String attrName, String attrValue, String valueStr) {
		if (valueStr == null) {
			return;
		}
		if (valueStr.startsWith("res/")) {
			// remove duplicated resources.
			return;
		}
		cw.startLine();
		cw.add('<').add(itemTag);
		if (attrName != null && attrValue != null) {
			if (typeName.equals("attr")) {
				cw.add(' ').add("name=\"").add(attrName.replace("id.", "")).add("\" value=\"").add(attrValue).add("\"");
			} else if (typeName.equals("style")) {
				cw.add(' ').add("name=\"").add(attrName.replace("attr.", "")).add("\"");
			} else {
				cw.add(' ').add(attrName).add("=\"").add(attrValue).add('"');
			}
		}
		if (valueStr.equals("")) {
			cw.add(" />");
		} else {
			cw.add('>');
			if (itemTag.equals("string")) {
				cw.add(StringUtils.escapeResStrValue(valueStr));
			} else {
				cw.add(StringUtils.escapeResValue(valueStr));
			}
			cw.add("</").add(itemTag).add('>');
		}
	}

	private String getFileName(ResourceEntry ri) {
		StringBuilder sb = new StringBuilder();
		String locale = ri.getConfig().getLocale();
		sb.append("res/values");
		if (!locale.isEmpty()) {
			sb.append('-').append(locale);
		}
		sb.append('/');
		sb.append(ri.getTypeName());
		if (!ri.getTypeName().endsWith("s")) {
			sb.append('s');
		}
		sb.append(".xml");
		return sb.toString();
	}
}
