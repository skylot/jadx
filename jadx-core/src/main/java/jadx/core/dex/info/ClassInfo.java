package jadx.core.dex.info;

import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
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

	private ClassInfo(DexNode dex, ArgType type) {
		if (!type.isObject()) {
			throw new JadxRuntimeException("Not class type: " + type);
		}
		this.type = type;

		splitNames(dex, true);
	}

	public static ClassInfo fromType(DexNode dex, ArgType type) {
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
		ArgType type = dex.getType(clsIndex);
		if (type.isArray()) {
			type = ArgType.OBJECT;
		}
		return fromType(dex, type);
	}

	public static ClassInfo fromName(DexNode dex, String clsName) {
		return fromType(dex, ArgType.object(clsName));
	}

	private void splitNames(DexNode dex, boolean canBeInner) {
		String fullObjectName = type.getObject();
		assert fullObjectName.indexOf('/') == -1 : "Raw type: " + type;

		String clsName;
		int dot = fullObjectName.lastIndexOf('.');
		if (dot == -1) {
			// rename default package if it used from class with package (often for obfuscated apps),
			pkg = Consts.DEFAULT_PACKAGE_NAME;
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

		char firstChar = clsName.charAt(0);
		if (Character.isDigit(firstChar)) {
			clsName = Consts.ANONYMOUS_CLASS_PREFIX + clsName;
		} else if (firstChar == '$') {
			clsName = "_" + clsName;
		}
		if (NameMapper.isReserved(clsName)) {
			clsName += "_";
		}
		this.fullName = (parentClass != null ? parentClass.getFullName() : pkg) + "." + clsName;
		this.name = clsName;
	}

	public String getFullPath() {
		return pkg.replace('.', File.separatorChar)
				+ File.separatorChar
				+ getNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return fullName;
	}

	public boolean isObject() {
		return fullName.equals(Consts.CLASS_OBJECT);
	}

	public String getShortName() {
		return name;
	}

	public String getRawName() {
		return type.getObject();
	}

	public String getPackage() {
		return pkg;
	}

	public boolean isPackageDefault() {
		return pkg.isEmpty() || pkg.equals(Consts.DEFAULT_PACKAGE_NAME);
	}

	public String getNameWithoutPackage() {
		return (parentClass != null ? parentClass.getNameWithoutPackage() + "." : "") + name;
	}

	public ClassInfo getParentClass() {
		return parentClass;
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
			return this.getFullName().equals(other.getFullName());
		}
		return false;
	}
}
