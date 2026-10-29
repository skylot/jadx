package jadx.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jadx.core.codegen.ClassNames;
import jadx.core.dex.nodes.ClassNode;

public class CacheStorage {

	private final Map<ClassNode, ClassNames> classNames = new ConcurrentHashMap<>();

	public ClassNames getClassNames(ClassNode cls) {
		return classNames.get(cls.getTopParentClass());
	}

	public void putClassNames(ClassNode cls, ClassNames names) {
		classNames.put(cls.getTopParentClass(), names);
	}

	public void clearClassNames() {
		classNames.clear();
	}
}
