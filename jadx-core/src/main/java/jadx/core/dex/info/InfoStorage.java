package jadx.core.dex.info;

import java.util.HashMap;
import java.util.Map;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;

public class InfoStorage {

	private final Map<ArgType, ClassInfo> classes = new HashMap<>();
	private final Map<Integer, MethodInfo> methods = new HashMap<>();
	private final Map<FieldInfo, FieldInfo> fields = new HashMap<>();

	public ClassInfo getCls(ArgType type) {
		return classes.get(type);
	}

	public ClassInfo putCls(ClassInfo cls) {
		synchronized (classes) {
			ClassInfo prev = classes.put(cls.getType(), cls);
			return prev == null ? cls : prev;
		}
	}

	private int generateMethodLookupId(DexNode dex, int mthId) {
		return dex.getDexId() << 16 | mthId;
	}

	public MethodInfo getMethod(DexNode dex, int mtdId) {
		return methods.get(generateMethodLookupId(dex, mtdId));
	}

	public MethodInfo putMethod(DexNode dex, int mthId, MethodInfo mth) {
		synchronized (methods) {
			MethodInfo prev = methods.put(generateMethodLookupId(dex, mthId), mth);
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
