package jadx.core.dex.info;

import java.util.HashMap;
import java.util.Map;

import jadx.core.dex.instructions.args.ArgType;

public class InfoStorage {

	private final Map<ArgType, ClassInfo> classes = new HashMap<>();
	private final Map<FieldInfo, FieldInfo> fields = new HashMap<>();
	// use only one MethodInfo instance
	private final Map<MethodInfo, MethodInfo> uniqueMethods = new HashMap<>();

	public ClassInfo getCls(ArgType type) {
		return classes.get(type);
	}

	public ClassInfo putCls(ClassInfo cls) {
		synchronized (classes) {
			ClassInfo prev = classes.put(cls.getType(), cls);
			return prev == null ? cls : prev;
		}
	}

	public MethodInfo putMethod(MethodInfo newMth) {
		synchronized (uniqueMethods) {
			MethodInfo prev = uniqueMethods.get(newMth);
			if (prev != null) {
				return prev;
			}
			uniqueMethods.put(newMth, newMth);
			return newMth;
		}
	}

	public FieldInfo getField(FieldInfo field) {
		synchronized (fields) {
			FieldInfo f = fields.get(field);
			if (f != null) {
				return f;
			}
			fields.put(field, field);
			return field;
		}
	}
}
