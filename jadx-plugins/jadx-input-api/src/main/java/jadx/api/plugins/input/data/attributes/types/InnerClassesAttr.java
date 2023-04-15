package jadx.api.plugins.input.data.attributes.types;

import java.util.Map;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class InnerClassesAttr extends PinnedAttribute {

	private final Map<String, InnerClsInfo> map;

	public InnerClassesAttr(Map<String, InnerClsInfo> map) {
		this.map = map;
	}

	public Map<String, InnerClsInfo> getMap() {
		return map;
	}

	@Override
	public IJadxAttrType<InnerClassesAttr> getAttrType() {
		return JadxAttrType.INNER_CLASSES;
	}

	@Override
	public String toString() {
		return "INNER_CLASSES:" + map;
	}
}
