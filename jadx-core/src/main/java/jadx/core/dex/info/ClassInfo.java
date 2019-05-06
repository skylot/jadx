package jadx.core.dex.info;

import java.io.File;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class ClassInfo implements Comparable<ClassInfo> {

	private final ArgType type;
	private String pkg;
	private String name;
	private String fullName;
	// for inner class not equals null
	private ClassInfo parentClass;
	// class info after rename (deobfuscation)
	private ClassInfo alias;

	private ClassInfo(ArgType type) {
		this.type = checkClassType(type);
		this.alias = this;
	}

	private ClassInfo(RootNode root, ArgType type) {
		this(root, type, true);
	}

	private ClassInfo(RootNode root, ArgType type, boolean inner) {
		this(type);
		splitAndApplyNames(root, type, inner);
	}

	private ClassInfo(ArgType type, String pkg, String name, @Nullable ClassInfo parentClass) {
		this(type);
		this.pkg = pkg;
		this.name = name;
		this.parentClass = parentClass;
		this.fullName = makeFullClsName(name, false);
	}

	public static ClassInfo fromType(RootNode root, ArgType type) {
		ArgType clsType = checkClassType(type);
		ClassInfo cls = root.getInfoStorage().getCls(clsType);
		if (cls != null) {
			return cls;
		}
		ClassInfo newClsInfo = new ClassInfo(root, clsType);
		return root.getInfoStorage().putCls(newClsInfo);
	}

	private static ArgType checkClassType(ArgType type) {
		if (type == null) {
			throw new JadxRuntimeException("Null class type");
		}
		if (type.isArray()) {
			// TODO: check case with method declared in array class like ( clone in int[])
			return ArgType.OBJECT;
		}
		if (!type.isObject() || type.isGenericType()) {
			throw new JadxRuntimeException("Not class type: " + type);
		}
		if (type.isGeneric()) {
			return ArgType.object(type.getObject());
		}
		return type;
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

	public void rename(RootNode root, String fullName) {
		if (!alias.makeFullName().equals(fullName)) {
			ClassInfo newAlias = new ClassInfo(type);
			newAlias.splitAndApplyNames(root, fullName, isInner());
			newAlias.alias = null;
			this.alias = newAlias;
		}
	}

	public void renameShortName(String aliasName) {
		if (!Objects.equals(name, aliasName)) {
			ClassInfo newAlias = new ClassInfo(type, alias.pkg, aliasName, parentClass);
			newAlias.alias = null;
			this.alias = newAlias;
		}
	}

	public void renamePkg(String aliasPkg) {
		if (!Objects.equals(pkg, aliasPkg)) {
			ClassInfo newAlias = new ClassInfo(type, aliasPkg, alias.name, parentClass);
			newAlias.alias = null;
			this.alias = newAlias;
		}
	}

	public boolean isRenamed() {
		return alias != this;
	}

	public ClassInfo getAlias() {
		return alias == null ? this : alias;
	}

	public boolean isAlias() {
		return alias == null;
	}

	private void splitAndApplyNames(RootNode root, ArgType type, boolean canBeInner) {
		splitAndApplyNames(root, type.getObject(), canBeInner);
	}

	private void splitAndApplyNames(RootNode root, String fullObjectName, boolean canBeInner) {
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
			String parClsName = pkg + '.' + clsName.substring(0, sep);
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

	private String makeFullClsName(String shortName, boolean raw) {
		if (parentClass != null) {
			String innerSep = raw ? "$" : ".";
			return parentClass.makeFullClsName(parentClass.getShortName(), raw) + innerSep + shortName;
		}
		return pkg.isEmpty() ? shortName : pkg + '.' + shortName;
	}

	public String makeFullName() {
		return makeFullClsName(this.name, false);
	}

	public String makeRawFullName() {
		return makeFullClsName(this.name, true);
	}

	public String getFullPath() {
		ClassInfo usedAlias = getAlias();
		return usedAlias.getPackage().replace('.', File.separatorChar)
				+ File.separatorChar
				+ usedAlias.getNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return makeFullName();
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
		return parentClass.getNameWithoutPackage() + '.' + name;
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
		this.parentClass = null;
		splitAndApplyNames(root, type, false);
	}

	public void updateNames(RootNode root) {
		splitAndApplyNames(root, type, isInner());
	}

	public ArgType getType() {
		return type;
	}

	@Override
	public String toString() {
		return makeFullName();
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

	@Override
	public int compareTo(@NotNull ClassInfo o) {
		return fullName.compareTo(o.fullName);
	}
}
