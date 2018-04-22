package jadx.core.dex.info;

import java.io.File;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class ClassInfo {

	private final ArgType type;
	private String pkg;
	private String name;
	private String fullName;
	// for inner class not equals null
	private ClassInfo parentClass;
	// class info after rename (deobfuscation)
	private ClassInfo alias;

	private ClassInfo(RootNode root, ArgType type) {
		this(root, type, true);
	}

	private ClassInfo(RootNode root, ArgType type, boolean inner) {
		if (!type.isObject() || type.isGeneric()) {
			throw new JadxRuntimeException("Not class type: " + type);
		}
		this.type = type;
		this.alias = this;

		splitNames(root, inner);
	}

	public static ClassInfo fromType(RootNode root, ArgType type) {
		if (type.isArray()) {
			type = ArgType.OBJECT;
		}
		ClassInfo cls = root.getInfoStorage().getCls(type);
		if (cls != null) {
			return cls;
		}
		cls = new ClassInfo(root, type);
		return root.getInfoStorage().putCls(cls);
	}

	public static ClassInfo fromDex(DexNode dex, int clsIndex) {
		if (clsIndex == DexNode.NO_INDEX) {
			return null;
		}
		return fromType(dex.root(), dex.getType(clsIndex));
	}

	public static ClassInfo fromName(RootNode root, String clsName) {
		return fromType(root, ArgType.object(clsName));
	}

	public static ClassInfo extCls(RootNode root, ArgType type) {
		ClassInfo classInfo = fromName(root, type.getObject());
		return classInfo.alias;
	}

	public void rename(RootNode root, String fullName) {
		ClassInfo newAlias = new ClassInfo(root, ArgType.object(fullName), isInner());
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

	private void splitNames(RootNode root, boolean canBeInner) {
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
			if (pkg.isEmpty()) {
				parClsName = clsName.substring(0, sep);
			}

			parentClass = fromName(root, parClsName);
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
		ClassInfo usedAlias = getAlias();
		return usedAlias.getPackage().replace('.', File.separatorChar)
				+ File.separatorChar
				+ usedAlias.getNameWithoutPackage().replace('.', '_');
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

	public boolean isDefaultPackage() {
		return pkg.isEmpty();
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

	public void notInner(RootNode root) {
		splitNames(root, false);
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
		return type.hashCode();
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
