package jadx.api.plugins.input.data.attributes.types;

import java.util.List;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.AccessFlagsScope;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class MethodParametersAttr extends PinnedAttribute {

	public static class Info {
		private final int accFlags;
		private final String name;

		public Info(int accFlags, String name) {
			this.accFlags = accFlags;
			this.name = name;
		}

		public int getAccFlags() {
			return accFlags;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			return AccessFlags.format(accFlags, AccessFlagsScope.METHOD) + name;
		}
	}

	private final List<Info> list;

	public MethodParametersAttr(List<Info> list) {
		this.list = list;
	}

	public List<Info> getList() {
		return list;
	}

	@Override
	public IJadxAttrType<MethodParametersAttr> getAttrType() {
		return JadxAttrType.METHOD_PARAMETERS;
	}

	@Override
	public String toString() {
		return "METHOD_PARAMETERS: " + list;
	}
}
