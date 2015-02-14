package jadx.core.dex.info;

import jadx.core.dex.instructions.args.ArgType;

import java.util.HashMap;
import java.util.Map;

public class InfoStorage {

	private final Map<ArgType, ClassInfo> classes = new HashMap<ArgType, ClassInfo>();
	private final Map<Integer, MethodInfo> methods = new HashMap<Integer, MethodInfo>();
	private final Map<FieldInfo, FieldInfo> fields = new HashMap<FieldInfo, FieldInfo>();

	public ClassInfo getCls(ArgType type) {
		return classes.get(type);
	}

	public ClassInfo putCls(ClassInfo cls) {
		synchronized (classes) {
			ClassInfo prev = classes.put(cls.getType(), cls);
			return prev == null ? cls : prev;
		}
	}

	public MethodInfo getMethod(int mtdId) {
		return methods.get(mtdId);
	}

	public MethodInfo putMethod(int mthId, MethodInfo mth) {
		synchronized (methods) {
			MethodInfo prev = methods.put(mthId, mth);
			return prev == null ? mth : prev;
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
