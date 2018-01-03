package jadx.gui.utils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;

public class CodeLinesInfo {
	private NavigableMap<Integer, JavaNode> map = new TreeMap<>();

	public CodeLinesInfo(JavaClass cls) {
		addClass(cls);
	}

	public void addClass(JavaClass cls) {
		map.put(cls.getDecompiledLine(), cls);
		for (JavaClass innerCls : cls.getInnerClasses()) {
			map.put(innerCls.getDecompiledLine(), innerCls);
			addClass(innerCls);
		}
		for (JavaMethod mth : cls.getMethods()) {
			map.put(mth.getDecompiledLine(), mth);
		}
	}

	public JavaNode getJavaNodeByLine(int line) {
		Map.Entry<Integer, JavaNode> entry = map.floorEntry(line);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}
}
