package jadx.plugins.input.java.data.attributes.debuginfo;

import java.util.HashMap;
import java.util.Map;

import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class LineNumberTableAttr implements IJavaAttribute {
	private final Map<Integer, Integer> lineMap;

	public LineNumberTableAttr(Map<Integer, Integer> sourceLineMap) {
		this.lineMap = sourceLineMap;
	}

	public Map<Integer, Integer> getLineMap() {
		return lineMap;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> {
			int len = reader.readU2();
			Map<Integer, Integer> map = new HashMap<>(len);
			for (int i = 0; i < len; i++) {
				int offset = reader.readU2();
				int line = reader.readU2();
				map.put(offset, line);
			}
			return new LineNumberTableAttr(map);
		};
	}
}
