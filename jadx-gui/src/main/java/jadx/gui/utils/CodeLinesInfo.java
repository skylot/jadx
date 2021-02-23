package jadx.gui.utils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;

public class CodeLinesInfo {
	private final NavigableMap<Integer, JavaNode> map = new TreeMap<>();

	public CodeLinesInfo(JavaClass cls) {
		addClass(cls, false);
	}

	public CodeLinesInfo(JavaClass cls, boolean includeFields) {
		addClass(cls, includeFields);
	}

	private void addClass(JavaClass cls, boolean includeFields) {
		map.put(cls.getDecompiledLine(), cls);
		for (JavaClass innerCls : cls.getInnerClasses()) {
			map.put(innerCls.getDecompiledLine(), innerCls);
			addClass(innerCls, includeFields);
		}
		for (JavaMethod mth : cls.getMethods()) {
			map.put(mth.getDecompiledLine(), mth);
		}
		if (includeFields) {
			for (JavaField field : cls.getFields()) {
				map.put(field.getDecompiledLine(), field);
			}
		}
	}

	public JavaNode getJavaNodeByLine(int line) {
		Map.Entry<Integer, JavaNode> entry = map.floorEntry(line);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}

	public JavaNode getJavaNodeBelowLine(int line) {
		Map.Entry<Integer, JavaNode> entry = map.ceilingEntry(line);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}

	public JavaNode getDefAtLine(int line) {
		return map.get(line);
	}
}
