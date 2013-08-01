package jadx.core.dex.info;

import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClassInfo {

	private static final Map<ArgType, ClassInfo> CLASSINFO_CACHE = new WeakHashMap<ArgType, ClassInfo>();

	public static ClassInfo fromDex(DexNode dex, int clsIndex) {
		if (clsIndex == DexNode.NO_INDEX)
			return null;

		ArgType type = dex.getType(clsIndex);
		if (type.isArray())
			type = ArgType.OBJECT;

		return fromType(type);
	}

	public static ClassInfo fromName(String clsName) {
		return fromType(ArgType.object(clsName));
	}

	public static ClassInfo fromType(ArgType type) {
		ClassInfo cls = CLASSINFO_CACHE.get(type);
		if (cls == null) {
			cls = new ClassInfo(type);
			CLASSINFO_CACHE.put(type, cls);
		}
		return cls;
	}

	public static void clearCache() {
		CLASSINFO_CACHE.clear();
	}

	private final ArgType type;
	private String pkg;
	private String name;
	private String fullName;
	private ClassInfo parentClass; // not equals null if this is inner class

	private ClassInfo(ArgType type) {
		assert type.isObject() : "Not class type: " + type;
		this.type = type;

		splitNames(true);
	}

	private void splitNames(boolean canBeInner) {
		String fullObjectName = type.getObject();
		assert fullObjectName.indexOf('/') == -1 : "Raw type: " + type;

		String name;
		int dot = fullObjectName.lastIndexOf('.');
		if (dot == -1) {
			// rename default package if it used from class with package (often for obfuscated apps),
			pkg = Consts.DEFAULT_PACKAGE_NAME;
			name = fullObjectName;
		} else {
			pkg = fullObjectName.substring(0, dot);
			name = fullObjectName.substring(dot + 1);
		}

		int sep = name.lastIndexOf('$');
		if (canBeInner && sep > 0 && sep != name.length() - 1) {
			String parClsName = pkg + '.' + name.substring(0, sep);
			parentClass = fromName(parClsName);
			name = name.substring(sep + 1);
		} else {
			parentClass = null;
		}

		char firstChar = name.charAt(0);
		if (Character.isDigit(firstChar)) {
			name = "InnerClass_" + name;
		} else if(firstChar == '$') {
			name = "_" + name;
		}
		if (NameMapper.isReserved(name)) {
			name += "_";
		}
		this.fullName = (parentClass != null ? parentClass.getFullName() : pkg) + "." + name;
		this.name = name;
	}

	public String getFullPath() {
		return pkg.replace('.', File.separatorChar)
				+ File.separatorChar
				+ getNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return fullName;
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

	public void notInner() {
		splitNames(false);
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
		if (this == obj) return true;
		if (obj instanceof ClassInfo) {
			ClassInfo other = (ClassInfo) obj;
			return this.getFullName().equals(other.getFullName());
		}
		return false;
	}
}
