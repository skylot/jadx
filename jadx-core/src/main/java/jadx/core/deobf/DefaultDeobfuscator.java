package jadx.core.deobf;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDeobfuscator implements IDeobfuscator {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultDeobfuscator.class);

	private static final boolean DEBUG = false;
	
	public static final char classNameSepearator = '.';

	private int maxLength = 40;
	private int minLength = 2;

	private int pkgIndex = 0;
	private int clsIndex = 0;

	private List<DexNode> dexNodes;
	private static PackageNode rootPackage = new PackageNode("");

	private static final String MAP_FILE_CHARSET = "UTF-8";

	/**
	 * Gets package node for full package name
	 * 
	 * @param fullPkgName full package name
	 * @param _creat if {@code true} then will create all absent objects 
	 * 
	 * @return package node object or {@code null} if no package found and <b>_creat</b> set to {@code false}
	 */
	public static PackageNode getPackageNode(String fullPkgName, boolean _creat) {
		if (fullPkgName.isEmpty() || fullPkgName.equals(classNameSepearator)) {
			return rootPackage;
		}

		PackageNode result = rootPackage;
		PackageNode parentNode;
		do {
			String pkgName;
			int idx = fullPkgName.indexOf(classNameSepearator);

			if (idx > -1) {
				pkgName = fullPkgName.substring(0, idx);
				fullPkgName = fullPkgName.substring(idx+1);
			} else {
				pkgName = fullPkgName;
				fullPkgName = "";
			}

			parentNode = result;
			result = result.getInnerPackageByName(pkgName);
			if ((result == null) && (_creat)) {
				result = new PackageNode(pkgName);
				parentNode.addInnerPackage(result);
			}
		} while (!fullPkgName.isEmpty() && (result != null));

		return result;
	}

	private class DefaultDeobfuscatorClassInfo {
		public ClassNode cls;
		public PackageNode pkg;
		public String alias;

		public DefaultDeobfuscatorClassInfo(ClassNode cls, PackageNode pkg) {
			this.cls = cls;
			this.pkg = pkg;
		}

		public String getNameWithoutPackage(DefaultDeobfuscatorClassInfo deobfClsInfo) {
			final ClassNode clsNode = deobfClsInfo.cls;
			String prefix;
			ClassNode parentClass = clsNode.getParentClass();
			if (parentClass != clsNode) {
				DefaultDeobfuscatorClassInfo parentDeobfClassInfo = DefaultDeobfuscator.clsMap.get(parentClass.getClassInfo());

				if (parentDeobfClassInfo != null) {
					prefix = getNameWithoutPackage(parentDeobfClassInfo) + DefaultDeobfuscator.classNameSepearator;
				} else {
					prefix = DefaultDeobfuscator.getNameWithoutPackage(parentClass.getClassInfo()) + DefaultDeobfuscator.classNameSepearator;
				}
			} else {
				prefix = "";
			}

			return prefix + ((deobfClsInfo.alias != null) ? deobfClsInfo.alias : deobfClsInfo.cls.getShortName());
		}

		public String getFullName() {
			return pkg.getFullAlias() + DefaultDeobfuscator.classNameSepearator + getNameWithoutPackage(this);
		}
	}

	private Map<String, String> preloadClsMap = Collections.emptyMap();
	private static Map<ClassInfo, DefaultDeobfuscatorClassInfo> clsMap = new HashMap<ClassInfo, DefaultDeobfuscatorClassInfo>();
	
	public static String getNameWithoutPackage(ClassInfo clsInfo) {
		String prefix;
		ClassInfo parentClsInfo = clsInfo.getParentClass();
		if (parentClsInfo != null) {
			DefaultDeobfuscatorClassInfo parentDeobfClsInfo = DefaultDeobfuscator.clsMap.get(parentClsInfo);

			if (parentDeobfClsInfo != null) {
				prefix = parentDeobfClsInfo.getNameWithoutPackage(parentDeobfClsInfo) + DefaultDeobfuscator.classNameSepearator;
			} else {
				prefix = getNameWithoutPackage(parentClsInfo) + DefaultDeobfuscator.classNameSepearator;
			}
		} else {
			prefix = "";
		}
		return prefix + clsInfo.getShortName();
	}

	private void doClass(ClassNode cls) {
		final String pkgFullName = cls.getPackage();

		PackageNode pkg = getPackageNode(pkgFullName, true);
		doPkg(pkg, pkgFullName);

		if (preloadClsMap.containsKey(cls.getFullName())) {
			DefaultDeobfuscatorClassInfo clsInfo = new DefaultDeobfuscatorClassInfo(cls, pkg);
			clsInfo.alias = preloadClsMap.get(cls.getFullName());
			clsMap.put(cls.getClassInfo(), clsInfo);
			return;
		}

		if (clsMap.containsKey(cls)) {
			return;
		}

		final String className = cls.getShortName();
		if (shouldRename(className)) {
			DefaultDeobfuscatorClassInfo clsInfo = new DefaultDeobfuscatorClassInfo(cls, pkg);

			clsInfo.alias = String.format("C%04d%s", clsIndex++, short4LongName(className));
			clsMap.put(cls.getClassInfo(), clsInfo);
		}
	}

	private String short4LongName(String name) {
		if (name.length() > maxLength) {
			return "x" + Integer.toHexString(name.hashCode());
		} else {
			return name;
		}
	}

	private Set<String> pkgSet = new TreeSet<String>();

	private void doPkg(PackageNode pkg, String fullName) {
		if (pkgSet.contains(fullName)) {
			return;
		}
		pkgSet.add(fullName);

		// doPkg for all parent packages except root that not hasAlisas
		PackageNode parentPkg = pkg.getParentPackage();
		while (!parentPkg.getName().isEmpty()) {
			if (!parentPkg.hasAlias()) {
				doPkg(parentPkg, parentPkg.getFullName());
			}
			parentPkg = parentPkg.getParentPackage();
		}

		final String pkgName = pkg.getName();
		if (shouldRename(pkgName) && !pkg.hasAlias()) {
			final String pkgAlias = String.format("p%03d%s", pkgIndex++, short4LongName(pkgName));
			pkg.setAlias(pkgAlias);
		}
	}

	private void preprocess() {
		if (dexNodes != null) {
			for (DexNode dexNode : dexNodes) {
				for (ClassNode cls : dexNode.getClasses()) {
					doClass(cls);
				}
			}
		}
	}

	private boolean shouldRename(String s) {
		return s.length() > maxLength || s.length() < minLength || NameMapper.isReserved(s);
	}

	private void dumpClassAlias(ClassNode cls) {
		PackageNode pkg = getPackageNode(cls.getPackage(), false);

		if (pkg != null) {
			if (!cls.getFullName().equals(getClassFullName(cls))) {
				LOG.info("Alias name for class '{}' is '{}'", cls.getFullName(), getClassFullName(cls));
			}
		} else {
			LOG.error("Can't find package node for '{}'", cls.getPackage());
		}
	}

	private void dumpAlias() {
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				dumpClassAlias(cls);
			}
		}
	}

	/**
	 * Sets input data for processing
	 * 
	 * @param nodes
	 *  
	 * @return @{code this}
	 */
	public DefaultDeobfuscator setInputData(List<DexNode> nodes) {
		this.dexNodes = nodes;
		return this;
	}

	/**
	 * Sets minimum name length, if name length lesser than value, 
	 * DefaultDeobfuscator will work
	 * 
	 * @param value 
	 *  
	 * @return @{code this}
	 */
	public DefaultDeobfuscator setMinNameLength(int value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets maximum name length, if name length greater than value, 
	 * DefaultDeobfuscator will work
	 * 
	 * @param value
	 *  
	 * @return @{code this}
	 */
	public DefaultDeobfuscator setMaxNameLength(int value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Loads DefaultDeobfuscator presets
	 * 
	 * @param config
	 * @throws IOException
	 */
	public void load(File mapFile) throws IOException {
		if (mapFile.exists()) {
			List<String> lines = FileUtils.readLines(mapFile, MAP_FILE_CHARSET);

			for (String l : lines) {
				if (l.startsWith("p ")) {
					final String rule = l.substring(2);
					final String va[] = rule.split("=");

					if (va.length == 2) {
						PackageNode pkg = getPackageNode(va[0], true);
						pkg.setAlias(va[1]);
					}
				} else if (l.startsWith("c ")) {
					final String rule = l.substring(2);
					final String va[] = rule.split("=");

					if (va.length == 2) {
						if (preloadClsMap.isEmpty()) {
							preloadClsMap = new HashMap<String, String>();
						}
						preloadClsMap.put(va[0], va[1]);
					}
				}
			}
		}
	}

	public void process() {
		preprocess();
		if (DEBUG) {
			dumpAlias();
		}

		preloadClsMap.clear();
		preloadClsMap = Collections.emptyMap();
	}

	private static void dfsPackageName(List<String> list, String prefix, PackageNode node) {
		for (PackageNode pp : node.getInnerPackages()) {
			dfsPackageName(list, prefix + '.' + node.getName(), pp);
		}

		if (node.hasAlias()) {
			list.add(String.format("p %s.%s=%s", prefix, node.getName(), node.getAlias()));
		}
	}

	/**
	 * Saves DefaultDeobfuscator presets
	 * 
	 * @param mapFile
	 * @throws IOException
	 */
	public void save(File mapFile) throws IOException {
		List<String> list = new ArrayList<String>();

		// packages
		for (PackageNode p : rootPackage.getInnerPackages()) {
			for (PackageNode pp : p.getInnerPackages()) {
				dfsPackageName(list, p.getName(), pp);
			}

			if (p.hasAlias()) {
				list.add(String.format("p %s=%s", p.getName(), p.getAlias()));
			}
		}

		// classes
		for (DefaultDeobfuscatorClassInfo deobfClsInfo : clsMap.values()) {
			if (deobfClsInfo.alias != null) {
				list.add(String.format("c %s=%s", deobfClsInfo.cls.getFullName(), deobfClsInfo.alias));
			}
		}

		FileUtils.writeLines(mapFile, MAP_FILE_CHARSET, list);
		list.clear();
	}


	@Override
	public String getPackageName(String packageName) {
		final PackageNode pkg = getPackageNode(packageName, false);
		if (pkg != null) {
			return pkg.getFullAlias();
		}
		return packageName;
	}

	@Override
	public String getClassShortName(ClassNode cls) {
		return getClassShortName(cls.getClassInfo());
	}

	@Override
	public String getClassShortName(ClassInfo clsInfo) {
		final DefaultDeobfuscatorClassInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return (deobfClsInfo.alias != null) ? deobfClsInfo.alias : clsInfo.getShortName(); 
		}

		return clsInfo.getShortName();
	}

	@Override
	public String getClassName(ClassNode cls) {
		return getClassName(cls.getClassInfo());
	}

	@Override
	public String getClassName(ClassInfo clsInfo) {
		final DefaultDeobfuscatorClassInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.getNameWithoutPackage(deobfClsInfo);
		}

		return getNameWithoutPackage(clsInfo);
	}

	@Override
	public String getClassFullName(ClassNode cls) {
		return getClassFullName(cls.getClassInfo());
	}

	@Override
	public String getClassFullName(ClassInfo clsInfo) {
		final DefaultDeobfuscatorClassInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.getFullName();
		}

		return getPackageName(clsInfo.getPackage()) + DefaultDeobfuscator.classNameSepearator + getClassName(clsInfo);
	}

	@Override
	public String getClassFullPath(ClassInfo clsInfo) {
		final DefaultDeobfuscatorClassInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.pkg.getFullAlias().replace(DefaultDeobfuscator.classNameSepearator, File.separatorChar)
				+ File.separatorChar
				+ deobfClsInfo.getNameWithoutPackage(deobfClsInfo).replace(DefaultDeobfuscator.classNameSepearator, '_');
		}

		
		return getPackageName(clsInfo.getPackage()).replace('.', File.separatorChar)
				+ File.separatorChar
				+ clsInfo.getNameWithoutPackage().replace('.', '_');
	}
}
