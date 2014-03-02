package jadx.core.dex.info;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;

import com.android.dx.io.FieldId;

public class FieldInfo {

	private final ClassInfo declClass;
	private final String name;
	private final ArgType type;

	public FieldInfo(ClassInfo declClass, String name, ArgType type) {
		this.declClass = declClass;
		this.name = name;
		this.type = type;
	}

	public static FieldInfo fromDex(DexNode dex, int index) {
		FieldId field = dex.getFieldId(index);
		return new FieldInfo(
				ClassInfo.fromDex(dex, field.getDeclaringClassIndex()),
				dex.getString(field.getNameIndex()),
				dex.getType(field.getTypeIndex()));
	}

	public static String getNameById(DexNode dex, int ind) {
		return dex.getString(dex.getFieldId(ind).getNameIndex());
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FieldInfo fieldInfo = (FieldInfo) o;
		if (!name.equals(fieldInfo.name)) {
			return false;
		}
		if (!type.equals(fieldInfo.type)) {
			return false;
		}
		if (!declClass.equals(fieldInfo.declClass)) {
			return false;
		}
		return true;
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
