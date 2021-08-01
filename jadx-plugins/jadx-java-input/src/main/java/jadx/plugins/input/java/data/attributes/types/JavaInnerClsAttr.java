package jadx.plugins.input.java.data.attributes.types;

import java.util.HashMap;
import java.util.Map;

import jadx.api.plugins.input.data.attributes.types.InnerClassesAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClsInfo;
import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class JavaInnerClsAttr extends InnerClassesAttr implements IJavaAttribute {

	public JavaInnerClsAttr(Map<String, InnerClsInfo> map) {
		super(map);
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> {
			int len = reader.readU2();
			ConstPoolReader constPool = clsData.getConstPoolReader();
			Map<String, InnerClsInfo> clsMap = new HashMap<>(len);
			for (int i = 0; i < len; i++) {
				String innerCls = constPool.getClass(reader.readU2());
				int outerClsIdx = reader.readU2();
				String outerCls = outerClsIdx == 0 ? null : constPool.getClass(outerClsIdx);
				String name = constPool.getUtf8(reader.readU2());
				int accFlags = reader.readU2();
				clsMap.put(innerCls, new InnerClsInfo(innerCls, outerCls, name, accFlags));
			}
			return new JavaInnerClsAttr(clsMap);
		};
	}
}
