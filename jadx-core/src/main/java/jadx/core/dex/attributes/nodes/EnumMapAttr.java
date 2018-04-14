package jadx.core.dex.attributes.nodes;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.FieldNode;

public class EnumMapAttr implements IAttribute {

	public static class KeyValueMap {
		private final Map<Object, Object> map = new HashMap<>();

		public Object get(Object key) {
			return map.get(key);
		}

		void put(Object key, Object value) {
			map.put(key, value);
		}
	}

	@Nullable
	private Map<FieldNode, KeyValueMap> fieldsMap;

	@Nullable
	public KeyValueMap getMap(FieldNode field) {
		if (fieldsMap == null) {
			return null;
		}
		return fieldsMap.get(field);
	}

	public void add(FieldNode field, Object key, Object value) {
		KeyValueMap map = getMap(field);
		if (map == null) {
			map = new KeyValueMap();
			if (fieldsMap == null) {
				fieldsMap = new HashMap<>();
			}
			fieldsMap.put(field, map);
		}
		map.put(key, value);
	}

	public boolean isEmpty() {
		return fieldsMap == null || fieldsMap.isEmpty();
	}

	@Override
	public AType<EnumMapAttr> getType() {
		return AType.ENUM_MAP;
	}

	@Override
	public String toString() {
		return "Enum fields map: " + fieldsMap;
	}
}
