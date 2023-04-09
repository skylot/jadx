package jadx.core.xmlgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.utils.StringUtils;
import jadx.core.xmlgen.entry.ProtoValue;
import jadx.core.xmlgen.entry.RawNamedValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

import static jadx.core.xmlgen.ParserConstants.PLURALS_MAP;
import static jadx.core.xmlgen.ParserConstants.TYPE_REFERENCE;

public class ResXmlGen {

	/**
	 * Skip only file based resource type
	 */
	private static final Set<String> SKIP_RES_TYPES = new HashSet<>(Arrays.asList(
			"anim",
			"animator",
			"font",
			"id", // skip id type, it is usually auto generated when used this syntax "@+id/my_id"
			"interpolator",
			"layout",
			"menu",
			"mipmap",
			"navigation",
			"raw",
			"transition",
			"xml"));

	private final ResourceStorage resStorage;
	private final ValuesParser vp;

	public ResXmlGen(ResourceStorage resStorage, ValuesParser vp) {
		this.resStorage = resStorage;
		this.vp = vp;
	}

	public List<ResContainer> makeResourcesXml() {
		Map<String, ICodeWriter> contMap = new HashMap<>();
		for (ResourceEntry ri : resStorage.getResources()) {
			if (SKIP_RES_TYPES.contains(ri.getTypeName())) {
				continue;
			}
			String fn = getFileName(ri);
			ICodeWriter cw = contMap.get(fn);
			if (cw == null) {
				cw = new SimpleCodeWriter();
				cw.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				cw.startLine("<resources>");
				cw.incIndent();
				contMap.put(fn, cw);
			}
			addValue(cw, ri);
		}

		List<ResContainer> files = new ArrayList<>(contMap.size());
		for (Map.Entry<String, ICodeWriter> entry : contMap.entrySet()) {
			String fileName = entry.getKey();
			ICodeWriter content = entry.getValue();
			content.decIndent();
			content.startLine("</resources>");
			ICodeInfo codeInfo = content.finish();
			files.add(ResContainer.textResource(fileName, codeInfo));
		}
		Collections.sort(files);
		return files;
	}

	private void addValue(ICodeWriter cw, ResourceEntry ri) {
		if (ri.getProtoValue() != null) {
			ProtoValue protoValue = ri.getProtoValue();
			if (protoValue.getValue() != null && protoValue.getNamedValues() == null) {
				addSimpleValue(cw, ri.getTypeName(), ri.getTypeName(), "name", ri.getKeyName(), protoValue.getValue());
			} else {
				cw.startLine();
				cw.add('<').add(ri.getTypeName()).add(' ');
				String itemTag = "item";
				cw.add("name=\"").add(ri.getKeyName()).add('\"');
				if (ri.getTypeName().equals("attr") && protoValue.getValue() != null) {
					cw.add(" format=\"").add(protoValue.getValue()).add('\"');
				}
				if (protoValue.getParent() != null) {
					cw.add(" parent=\"").add(protoValue.getParent()).add('\"');
				}
				cw.add(">");

				cw.incIndent();
				for (ProtoValue value : protoValue.getNamedValues()) {
					addProtoItem(cw, itemTag, ri.getTypeName(), value);
				}
				cw.decIndent();
				cw.startLine().add("</").add(ri.getTypeName()).add('>');
			}
		} else if (ri.getSimpleValue() != null) {
			String valueStr = vp.decodeValue(ri.getSimpleValue());
			addSimpleValue(cw, ri.getTypeName(), ri.getTypeName(), "name", ri.getKeyName(), valueStr);
		} else {
			cw.startLine();
			cw.add('<').add(ri.getTypeName()).add(" name=\"");
			String itemTag = "item";
			if (ri.getTypeName().equals("attr") && !ri.getNamedValues().isEmpty()) {
				cw.add(ri.getKeyName());
				int type = ri.getNamedValues().get(0).getRawValue().getData();
				if ((type & ValuesParser.ATTR_TYPE_ENUM) != 0) {
					itemTag = "enum";
				} else if ((type & ValuesParser.ATTR_TYPE_FLAGS) != 0) {
					itemTag = "flag";
				}
				String formatValue = XmlGenUtils.getAttrTypeAsString(type);
				if (formatValue != null) {
					cw.add("\" format=\"").add(formatValue);
				}
			} else {
				cw.add(ri.getKeyName());
			}
			if (ri.getTypeName().equals("style") || ri.getParentRef() != 0) {
				cw.add("\" parent=\"");
				if (ri.getParentRef() != 0) {
					String parent = vp.decodeValue(TYPE_REFERENCE, ri.getParentRef());
					cw.add(parent);
				}
			}
			cw.add("\">");

			cw.incIndent();
			for (RawNamedValue value : ri.getNamedValues()) {
				addItem(cw, itemTag, ri.getTypeName(), value);
			}
			cw.decIndent();
			cw.startLine().add("</").add(ri.getTypeName()).add('>');
		}
	}

	private void addProtoItem(ICodeWriter cw, String itemTag, String typeName, ProtoValue protoValue) {
		String name = protoValue.getName();
		String value = protoValue.getValue();
		switch (typeName) {
			case "attr":
				if (name != null) {
					addSimpleValue(cw, typeName, itemTag, name, value, "");
				}
				break;
			case "style":
				if (name != null) {
					addSimpleValue(cw, typeName, itemTag, name, "", value);
				}
				break;
			case "plurals":
				addSimpleValue(cw, typeName, itemTag, "quantity", name, value);
				break;
			default:
				addSimpleValue(cw, typeName, itemTag, null, null, value);
				break;
		}
	}

	private void addItem(ICodeWriter cw, String itemTag, String typeName, RawNamedValue value) {
		String nameStr = vp.decodeNameRef(value.getNameRef());
		String valueStr = vp.decodeValue(value.getRawValue());
		int dataType = value.getRawValue().getDataType();

		if (!typeName.equals("attr")) {
			if (dataType == ParserConstants.TYPE_REFERENCE && (valueStr == null || valueStr.equals("0"))) {
				valueStr = "@null";
			}
			if (dataType == ParserConstants.TYPE_INT_DEC && nameStr != null) {
				try {
					int intVal = Integer.parseInt(valueStr);
					String newVal = ManifestAttributes.getInstance().decode(nameStr.replace("android:", "").replace("attr.", ""), intVal);
					if (newVal != null) {
						valueStr = newVal;
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			if (dataType == ParserConstants.TYPE_INT_HEX && nameStr != null) {
				try {
					int intVal = Integer.decode(valueStr);
					String newVal = ManifestAttributes.getInstance().decode(nameStr.replace("android:", "").replace("attr.", ""), intVal);
					if (newVal != null) {
						valueStr = newVal;
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		switch (typeName) {
			case "attr":
				if (nameStr != null) {
					addSimpleValue(cw, typeName, itemTag, nameStr, valueStr, "");
				}
				break;
			case "style":
				if (nameStr != null) {
					addSimpleValue(cw, typeName, itemTag, nameStr, "", valueStr);
				}
				break;
			case "plurals":
				final String quantity = PLURALS_MAP.get(value.getNameRef());
				addSimpleValue(cw, typeName, itemTag, "quantity", quantity, valueStr);
				break;
			default:
				addSimpleValue(cw, typeName, itemTag, null, null, valueStr);
				break;
		}
	}

	private void addSimpleValue(ICodeWriter cw, String typeName, String itemTag, String attrName, String attrValue, String valueStr) {
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
				cw.add(' ').add("name=\"").add(attrName.replace("id.", "")).add("\" value=\"").add(attrValue).add('"');
			} else if (typeName.equals("style")) {
				cw.add(' ').add("name=\"").add(attrName.replace("attr.", "")).add('"');
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
		String qualifiers = ri.getConfig();
		sb.append("res/values");
		if (!qualifiers.isEmpty()) {
			sb.append(qualifiers);
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
