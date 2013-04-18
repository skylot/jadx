package jadx.dex.info;

import jadx.deobf.NameMapper;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.DexNode;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClassInfo {

	private static final Map<ArgType, ClassInfo> CLASSINFO_CACHE = new WeakHashMap<ArgType, ClassInfo>();
	private static final String DEFAULT_PACKAGE_NAME = "defpackage";

	private final String clsName;
	private final String clsPackage;
	private final ArgType type;
	private final String fullName;

	private final ClassInfo parentClass; // not equals null if this is inner class

	public static ClassInfo fromDex(DexNode dex, int clsIndex) {
		if (clsIndex == DexNode.NO_INDEX)
			return null;

		ArgType type = dex.getType(clsIndex);
		if (type.isArray())
			type = ArgType.OBJECT;

		return fromType(dex, type);
	}

	public static ClassInfo fromName(DexNode dex, String clsName) {
		return fromType(dex, ArgType.object(clsName));
	}

	public static ClassInfo fromType(DexNode dex, ArgType type) {
		ClassInfo cls = CLASSINFO_CACHE.get(type);
		if (cls == null) {
			cls = new ClassInfo(dex, type);
			CLASSINFO_CACHE.put(type, cls);
		}
		return cls;
	}

	public static void clearCache() {
		CLASSINFO_CACHE.clear();
	}

	private ClassInfo(DexNode dex, ArgType type) {
		assert type.isObject() : "Not class type: " + type;
		this.type = type;

		String fullObjectName = type.getObject();
		assert fullObjectName.indexOf('/') == -1 : "Raw type: " + type;

		boolean notObfuscated = dex.root().getJadxArgs().isNotObfuscated();
		String name;
		String pkg;

		int dot = fullObjectName.lastIndexOf('.');
		if (dot == -1) {
			// rename default package if it used from class with package (often for obfuscated apps),
			pkg = (notObfuscated ? "" : DEFAULT_PACKAGE_NAME);
			name = fullObjectName;
		} else {
			pkg = fullObjectName.substring(0, dot);
			name = fullObjectName.substring(dot + 1);
		}

		int sep = name.lastIndexOf('$');
		if (sep > 0 && sep != name.length() - 1) {
			String parClsName = pkg + '.' + name.substring(0, sep);
			if (notObfuscated || dex.root().isClassExists(parClsName)) {
				parentClass = fromName(dex, parClsName);
				name = name.substring(sep + 1);
			} else {
				parentClass = null;
			}
		} else {
			parentClass = null;
		}

		if (Character.isDigit(name.charAt(0)))
			name = "InnerClass_" + name;

		if (NameMapper.isReserved(name))
			name += "_";

		this.fullName = (parentClass != null ? parentClass.getFullName() : pkg) + "." + name;
		this.clsName = name;
		this.clsPackage = pkg;
	}

	public String getFullPath() {
		return clsPackage.replace('.', File.separatorChar) + File.separatorChar
				+ getNameWithoutPackage().replace('.', '_');
	}

	public String getFullName() {
		return fullName;
	}

	public String getShortName() {
		return clsName;
	}

	public String getPackage() {
		return clsPackage;
	}

	public boolean isPackageDefault() {
		return clsPackage.isEmpty() || clsPackage.equals(DEFAULT_PACKAGE_NAME);
	}

	public String getNameWithoutPackage() {
		return (parentClass != null ? parentClass.getNameWithoutPackage() + "." : "") + clsName;
	}

	public ClassInfo getParentClass() {
		return parentClass;
	}

	public boolean isInner() {
		return parentClass != null;
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
		return this.getFullName().hashCode();
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
