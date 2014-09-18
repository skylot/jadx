package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;

import java.util.HashMap;
import java.util.Map;

public class EnumMapAttr implements IAttribute {

	private Map<Object, Object> map = new HashMap<Object, Object>();

	public Map<Object, Object> getMap() {
		return map;
	}

	@Override
	public AType<EnumMapAttr> getType() {
		return AType.ENUM_MAP;
	}

	@Override
	public String toString() {
		return "Enum fields map: " + map;
	}

}
