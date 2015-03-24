package jadx.core.dex.info;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;

import com.android.dex.FieldId;

public final class FieldInfo {

	private final ClassInfo declClass;
	private final String name;
	private final ArgType type;
	private String alias;

	private FieldInfo(ClassInfo declClass, String name, ArgType type) {
		this.declClass = declClass;
		this.name = name;
		this.type = type;
		this.alias = name;
	}

	public static FieldInfo from(DexNode dex, ClassInfo declClass, String name, ArgType type) {
		FieldInfo field = new FieldInfo(declClass, name, type);
		return dex.getInfoStorage().getField(field);
	}

	public static FieldInfo fromDex(DexNode dex, int index) {
		FieldId field = dex.getFieldId(index);
		return from(dex,
				ClassInfo.fromDex(dex, field.getDeclaringClassIndex()),
				dex.getString(field.getNameIndex()),
				dex.getType(field.getTypeIndex()));
	}

	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
	}

	public ClassInfo getDeclClass() {
		return declClass;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getFullId() {
		return declClass.getFullName() + "." + name + ":" + TypeGen.signature(type);
	}

	public boolean isRenamed() {
		return !name.equals(alias);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FieldInfo fieldInfo = (FieldInfo) o;
		return name.equals(fieldInfo.name)
				&& type.equals(fieldInfo.type)
				&& declClass.equals(fieldInfo.declClass);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + declClass.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return declClass + "." + name + " " + type;
	}
}
