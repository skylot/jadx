package jadx.core.dex.info;

import java.util.Objects;

import jadx.api.plugins.input.data.IFieldRef;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

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

	public static FieldInfo from(RootNode root, ClassInfo declClass, String name, ArgType type) {
		FieldInfo field = new FieldInfo(declClass, name, type);
		return root.getInfoStorage().getField(field);
	}

	public static FieldInfo fromRef(RootNode root, IFieldRef fieldRef) {
		ClassInfo declClass = ClassInfo.fromName(root, fieldRef.getParentClassType());
		FieldInfo field = new FieldInfo(declClass, fieldRef.getName(), ArgType.parse(fieldRef.getType()));
		return root.getInfoStorage().getField(field);
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

	public void removeAlias() {
		this.alias = name;
	}

	public boolean hasAlias() {
		return !Objects.equals(name, alias);
	}

	public String getFullId() {
		return declClass.getFullName() + '.' + name + ':' + TypeGen.signature(type);
	}

	public String getShortId() {
		return name + ':' + TypeGen.signature(type);
	}

	public String getRawFullId() {
		return declClass.makeRawFullName() + '.' + name + ':' + TypeGen.signature(type);
	}

	public boolean equalsNameAndType(FieldInfo other) {
		return name.equals(other.name) && type.equals(other.type);
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
		return declClass + "." + name + ' ' + type;
	}
}
