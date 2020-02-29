package jadx.core.dex.info;

import java.util.HashMap;
import java.util.Map;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class InfoStorage {

	private final Map<ArgType, ClassInfo> classes = new HashMap<>();
	private final Map<FieldInfo, FieldInfo> fields = new HashMap<>();
	private final Map<Integer, MethodInfo> methods = new HashMap<>();
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

	private static int generateMethodLookupId(DexNode dex, int mthId) {
		return dex.getDexId() << 16 | mthId;
	}

	public MethodInfo getMethod(DexNode dex, int mtdId) {
		synchronized (methods) {
			return methods.get(generateMethodLookupId(dex, mtdId));
		}
	}

	public MethodInfo putMethod(DexNode dex, int mthId, MethodInfo methodInfo) {
		synchronized (methods) {
			MethodInfo uniqueMethodInfo = putMethod(methodInfo);
			MethodInfo prev = methods.put(generateMethodLookupId(dex, mthId), uniqueMethodInfo);
			if (prev != null && prev != uniqueMethodInfo) {
				throw new JadxRuntimeException("Method lookup id collision: " + methodInfo + ", " + prev + ", " + uniqueMethodInfo);
			}
			return uniqueMethodInfo;
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
