package jadx.plugins.input.java.data.attributes.types;

import java.util.ArrayList;
import java.util.List;

import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;
import jadx.plugins.input.java.data.attributes.types.data.RawBootstrapMethod;

public class JavaBootstrapMethodsAttr implements IJavaAttribute {

	private final List<RawBootstrapMethod> list;

	public JavaBootstrapMethodsAttr(List<RawBootstrapMethod> list) {
		this.list = list;
	}

	public List<RawBootstrapMethod> getList() {
		return list;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> {
			int len = reader.readU2();
			List<RawBootstrapMethod> list = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				int methodHandleIdx = reader.readU2();
				int argsCount = reader.readU2();
				int[] args = new int[argsCount];
				for (int j = 0; j < argsCount; j++) {
					args[j] = reader.readU2();
				}
				list.add(new RawBootstrapMethod(methodHandleIdx, args));
			}
			return new JavaBootstrapMethodsAttr(list);
		};
	}
}
