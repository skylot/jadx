package jadx.core.dex.info;

import java.io.File;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class ClassInfo implements Comparable<ClassInfo> {
	private final ArgType type;
	private String name;
	@Nullable("for inner classes")
	private String pkg;
	private String fullName;
	@Nullable
	private ClassInfo parentClass;
	@Nullable
	private ClassAliasInfo alias;

	private ClassInfo(RootNode root, ArgType type, boolean inner) {
		this.type = type;
		splitAndApplyNames(root, type, inner);
	}

	public static ClassInfo fromType(RootNode root, ArgType type) {
		ArgType clsType = checkClassType(type);
		ClassInfo cls = root.getInfoStorage().getCls(clsType);
		if (cls != null) {
			return cls;
		}
		ClassInfo newClsInfo = new ClassInfo(root, clsType, true);
		return root.getInfoStorage().putCls(newClsInfo);
	}

	public static ClassInfo fromName(RootNode root, String clsName) {
		return fromType(root, ArgType.object(clsName));
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

	public void changeShortName(String aliasName) {
		if (!Objects.equals(name, aliasName)) {
			ClassAliasInfo newAlias = new ClassAliasInfo(getAliasPkg(), aliasName);
			fillAliasFullName(newAlias);
			this.alias = newAlias;
		} else {
			this.alias = null;
		}
	}

	public void changePkg(String aliasPkg) {
		if (isInner()) {
			throw new JadxRuntimeException("Can't change package for inner class");
		}
		if (!Objects.equals(getAliasPkg(), aliasPkg)) {
			ClassAliasInfo newAlias = new ClassAliasInfo(aliasPkg, getAliasShortName());
			fillAliasFullName(newAlias);
			this.alias = newAlias;
		}
	}

	private void fillAliasFullName(ClassAliasInfo alias) {
		if (parentClass == null) {
			alias.setFullName(makeFullClsName(alias.getPkg(), alias.getShortName(), null, true, false));
		}
	}

	public String getAliasPkg() {
		if (isInner()) {
			return parentClass.getAliasPkg();
		}
		return alias == null ? getPackage() : alias.getPkg();
	}

	public String getAliasShortName() {
		return alias == null ? getShortName() : alias.getShortName();
	}

	public String getAliasFullName() {
		if (alias != null) {
			String aliasFullName = alias.getFullName();
			if (aliasFullName == null) {
				return makeAliasFullName();
			}
			return aliasFullName;
		}
		if (parentClass != null && parentClass.hasAlias()) {
			return makeAliasFullName();
		}
		return getFullName();
	}

	public boolean hasAlias() {
		if (alias != null && !alias.getShortName().equals(getShortName())) {
			return true;
		}
		return parentClass != null && parentClass.hasAlias();
	}

	public void removeAlias() {
		this.alias = null;
	}

	private void splitAndApplyNames(RootNode root, ArgType type, boolean canBeInner) {
		String fullObjectName = type.getObject();
		String clsPkg;
		String clsName;
		int dot = fullObjectName.lastIndexOf('.');
		if (dot == -1) {
			clsPkg = "";
			clsName = fullObjectName;
		} else {
			clsPkg = fullObjectName.substring(0, dot);
			clsName = fullObjectName.substring(dot + 1);
		}

		int sep = clsName.lastIndexOf('$');
		if (canBeInner && sep > 0 && sep != clsName.length() - 1) {
			String parClsName = clsPkg + '.' + clsName.substring(0, sep);
			if (clsPkg.isEmpty()) {
				parClsName = clsName.substring(0, sep);
			}
			pkg = null;
			parentClass = fromName(root, parClsName);
			clsName = clsName.substring(sep + 1);
		} else {
			pkg = clsPkg;
			parentClass = null;
		}
		this.name = clsName;
		this.fullName = makeFullName();
	}

	private static String makeFullClsName(String pkg, String shortName, ClassInfo parentClass, boolean alias, boolean raw) {
		if (parentClass != null) {
			String parentFullName;
			char innerSep = raw ? '$' : '.';
			if (alias) {
				parentFullName = raw ? parentClass.makeAliasRawFullName() : parentClass.getAliasFullName();
			} else {
				parentFullName = raw ? parentClass.makeRawFullName() : parentClass.getFullName();
			}
			return parentFullName + innerSep + shortName;
		}
		return pkg.isEmpty() ? shortName : pkg + '.' + shortName;
	}

	private String makeFullName() {
		return makeFullClsName(pkg, name, parentClass, false, false);
	}

	public String makeRawFullName() {
		return makeFullClsName(pkg, name, parentClass, false, true);
	}

	public String makeAliasFullName() {
		return makeFullClsName(getAliasPkg(), getAliasShortName(), parentClass, true, false);
	}

	public String makeAliasRawFullName() {
		return makeFullClsName(getAliasPkg(), getAliasShortName(), parentClass, true, true);
	}

	public String getAliasFullPath() {
		return getAliasPkg().replace('.', File.separatorChar)
				+ File.separatorChar
				+ getAliasNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return fullName;
	}

	public String getShortName() {
		return name;
	}

	@NotNull
	public String getPackage() {
		if (parentClass != null) {
			return parentClass.getPackage();
		}
		if (pkg == null) {
			throw new JadxRuntimeException("Package is null for not inner class");
		}
		return pkg;
	}

	public boolean isDefaultPackage() {
		return getPackage().isEmpty();
	}

	public String getRawName() {
		return type.getObject();
	}

	public String getAliasNameWithoutPackage() {
		if (parentClass == null) {
			return getAliasShortName();
		}
		return parentClass.getAliasNameWithoutPackage() + '.' + getAliasShortName();
	}

	@Nullable
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
		splitAndApplyNames(root, type, false);
		this.parentClass = null;
	}

	public void convertToInner(ClassNode parent) {
		splitAndApplyNames(parent.root(), type, true);
		this.parentClass = parent.getClassInfo();
	}

	public void updateNames(RootNode root) {
		splitAndApplyNames(root, type, isInner());
	}

	public ArgType getType() {
		return type;
	}

	@Override
	public String toString() {
		return getFullName();
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
		return getFullName().compareTo(o.getFullName());
	}
}
