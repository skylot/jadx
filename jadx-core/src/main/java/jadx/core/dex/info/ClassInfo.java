package jadx.core.dex.info;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.io.File;

public final class ClassInfo {

	private final ArgType type;
	private String pkg;
	private String name;
	private String fullName;
	// for inner class not equals null
	private ClassInfo parentClass;
	// class info after rename (deobfuscation)
	private ClassInfo alias;

	private ClassInfo(DexNode dex, ArgType type) {
		this(dex, type, true);
	}

	private ClassInfo(DexNode dex, ArgType type, boolean inner) {
		if (!type.isObject() || type.isGeneric()) {
			throw new JadxRuntimeException("Not class type: " + type);
		}
		this.type = type;
		this.alias = this;

		splitNames(dex, inner);
	}

	public static ClassInfo fromType(DexNode dex, ArgType type) {
		if (type.isArray()) {
			type = ArgType.OBJECT;
		}
		ClassInfo cls = dex.getInfoStorage().getCls(type);
		if (cls != null) {
			return cls;
		}
		cls = new ClassInfo(dex, type);
		return dex.getInfoStorage().putCls(cls);
	}

	public static ClassInfo fromDex(DexNode dex, int clsIndex) {
		if (clsIndex == DexNode.NO_INDEX) {
			return null;
		}
		return fromType(dex, dex.getType(clsIndex));
	}

	public static ClassInfo fromName(DexNode dex, String clsName) {
		return fromType(dex, ArgType.object(clsName));
	}

	public static ClassInfo extCls(DexNode dex, ArgType type) {
		ClassInfo classInfo = fromName(dex, type.getObject());
		return classInfo.alias;
	}

	public void rename(DexNode dex, String fullName) {
		ClassInfo newAlias = new ClassInfo(dex, ArgType.object(fullName), isInner());
		if (!alias.getFullName().equals(newAlias.getFullName())) {
			this.alias = newAlias;
		}
	}

	public boolean isRenamed() {
		return alias != this;
	}

	public ClassInfo getAlias() {
		return alias;
	}

	private void splitNames(DexNode dex, boolean canBeInner) {
		String fullObjectName = type.getObject();
		String clsName;
		int dot = fullObjectName.lastIndexOf('.');
		if (dot == -1) {
			pkg = "";
			clsName = fullObjectName;
		} else {
			pkg = fullObjectName.substring(0, dot);
			clsName = fullObjectName.substring(dot + 1);
		}

		int sep = clsName.lastIndexOf('$');
		if (canBeInner && sep > 0 && sep != clsName.length() - 1) {
			String parClsName = pkg + "." + clsName.substring(0, sep);
			parentClass = fromName(dex, parClsName);
			clsName = clsName.substring(sep + 1);
		} else {
			parentClass = null;
		}
		this.name = clsName;
		this.fullName = makeFullClsName(clsName, false);
	}

	public String makeFullClsName(String shortName, boolean raw) {
		if (parentClass != null) {
			String innerSep = raw ? "$" : ".";
			return parentClass.makeFullClsName(parentClass.getShortName(), raw) + innerSep + shortName;
		}
		return pkg.isEmpty() ? shortName : pkg + "." + shortName;
	}

	public String getFullPath() {
		ClassInfo alias = getAlias();
		return alias.getPackage().replace('.', File.separatorChar)
				+ File.separatorChar
				+ alias.getNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return fullName;
	}

	public String getShortName() {
		return name;
	}

	public String getPackage() {
		return pkg;
	}

	public String getRawName() {
		return type.getObject();
	}

	public String getNameWithoutPackage() {
		if (parentClass == null) {
			return name;
		}
		return parentClass.getNameWithoutPackage() + "." + name;
	}

	public ClassInfo getParentClass() {
		return parentClass;
	}

	public ClassInfo getTopParentClass() {
		if (parentClass != null) {
			ClassInfo topCls = parentClass.getTopParentClass();
			return topCls != null ? topCls : parentClass;
		}
		return null;
	}

	public boolean isInner() {
		return parentClass != null;
	}

	public void notInner(DexNode dex) {
		splitNames(dex, false);
	}

	public ArgType getType() {
		return type;
	}

	@Override
	public String toString() {
		return fullName;
	}

	@Override
	public int hashCode() {
		return fullName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ClassInfo) {
			ClassInfo other = (ClassInfo) obj;
			return this.type.equals(other.type);
		}
		return false;
	}
}
