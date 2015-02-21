package jadx.core.deobf;

import jadx.api.IJadxArgs;
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

public class Deobfuscator {
	private static final Logger LOG = LoggerFactory.getLogger(Deobfuscator.class);

	private static final boolean DEBUG = false;

	private static final String MAP_FILE_CHARSET = "UTF-8";
	private static final String classNameSeparator = ".";
	private static final String innerClassSeparator = "$";

	private final Map<ClassInfo, DeobfClsInfo> clsMap = new HashMap<ClassInfo, DeobfClsInfo>();
	private final IJadxArgs args;
	private final File deobfMapFile;
	private final List<DexNode> dexNodes;

	private int maxLength = 40;
	private int minLength = 2;
	private int pkgIndex = 0;
	private int clsIndex = 0;

	private PackageNode rootPackage = new PackageNode("");
	private Map<String, String> preLoadClsMap = Collections.emptyMap();

	public Deobfuscator(IJadxArgs args, List<DexNode> dexNodes, File deobfMapFile) {
		this.args = args;
		this.dexNodes = dexNodes;
		this.deobfMapFile = deobfMapFile;

		this.minLength = args.getDeobfuscationMinLength();
		this.maxLength = args.getDeobfuscationMaxLength();
	}

	public void execute() {
		if (deobfMapFile.exists()) {
			try {
				load();
			} catch (IOException e) {
				LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.getAbsolutePath(), e);
			}
		}
		process();
		try {
			if (deobfMapFile.exists()) {
				if (args.isDeobfuscationForceSave()) {
					save();
				} else {
					LOG.warn("Deobfuscation map file '{}' exists. Use command line option '--deobf-rewrite-cfg' to rewrite it",
							deobfMapFile.getAbsolutePath());
				}
			} else {
				save();
			}
		} catch (IOException e) {
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.getAbsolutePath(), e);
		}
	}

	public void process() {
		preProcess();
		if (DEBUG) {
			dumpAlias();
		}
		preLoadClsMap.clear();
		preLoadClsMap = Collections.emptyMap();

		for (DexNode dexNode : dexNodes) {
			for (ClassNode classNode : dexNode.getClasses()) {
				ClassInfo clsInfo = classNode.getClassInfo();
				String fullName = getClassFullName(clsInfo);
				clsInfo.rename(dexNode, fullName);
			}
		}
	}

	/**
	 * Gets package node for full package name
	 *
	 * @param fullPkgName full package name
	 * @param create      if {@code true} then will create all absent objects
	 * @return package node object or {@code null} if no package found and <b>create</b> set to {@code false}
	 */
	public PackageNode getPackageNode(String fullPkgName, boolean create) {
		if (fullPkgName.isEmpty() || fullPkgName.equals(classNameSeparator)) {
			return rootPackage;
		}
		PackageNode result = rootPackage;
		PackageNode parentNode;
		do {
			String pkgName;
			int idx = fullPkgName.indexOf(classNameSeparator);

			if (idx > -1) {
				pkgName = fullPkgName.substring(0, idx);
				fullPkgName = fullPkgName.substring(idx + 1);
			} else {
				pkgName = fullPkgName;
				fullPkgName = "";
			}
			parentNode = result;
			result = result.getInnerPackageByName(pkgName);
			if ((result == null) && (create)) {
				result = new PackageNode(pkgName);
				parentNode.addInnerPackage(result);
			}
		} while (!fullPkgName.isEmpty() && (result != null));

		return result;
	}

	private final class DeobfClsInfo {
		public ClassNode cls;
		public PackageNode pkg;
		public String alias;

		public DeobfClsInfo(ClassNode cls, PackageNode pkg) {
			this.cls = cls;
			this.pkg = pkg;
		}

		public String makeNameWithoutPkg() {
			String prefix;
			ClassNode parentClass = cls.getParentClass();
			if (parentClass != cls) {
				DeobfClsInfo parentDeobfClsInfo = clsMap.get(parentClass.getClassInfo());
				if (parentDeobfClsInfo != null) {
					prefix = parentDeobfClsInfo.makeNameWithoutPkg();
				} else {
					prefix = getNameWithoutPackage(parentClass.getClassInfo());
				}
				prefix += innerClassSeparator;
			} else {
				prefix = "";
			}

			return prefix + ((this.alias != null) ? this.alias : this.cls.getShortName());
		}

		public String getFullName() {
			return pkg.getFullAlias() + classNameSeparator + makeNameWithoutPkg();
		}
	}

	public String getNameWithoutPackage(ClassInfo clsInfo) {
		String prefix;
		ClassInfo parentClsInfo = clsInfo.getParentClass();
		if (parentClsInfo != null) {
			DeobfClsInfo parentDeobfClsInfo = clsMap.get(parentClsInfo);
			if (parentDeobfClsInfo != null) {
				prefix = parentDeobfClsInfo.makeNameWithoutPkg();
			} else {
				prefix = getNameWithoutPackage(parentClsInfo);
			}
			prefix += innerClassSeparator;
		} else {
			prefix = "";
		}
		return prefix + clsInfo.getShortName();
	}

	private void doClass(ClassNode cls) {
		final String pkgFullName = cls.getClassInfo().getPackage();

		PackageNode pkg = getPackageNode(pkgFullName, true);
		doPkg(pkg, pkgFullName);

		if (preLoadClsMap.containsKey(cls.getClassInfo().getFullName())) {
			DeobfClsInfo clsInfo = new DeobfClsInfo(cls, pkg);
			clsInfo.alias = preLoadClsMap.get(cls.getFullName());
			clsMap.put(cls.getClassInfo(), clsInfo);
			return;
		}

		if (clsMap.containsKey(cls.getClassInfo())) {
			return;
		}

		final String className = cls.getClassInfo().getShortName();
		if (shouldRename(className)) {
			DeobfClsInfo clsInfo = new DeobfClsInfo(cls, pkg);
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

	private void preProcess() {
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
	 * Loads deobfuscator presets
	 *
	 * @throws IOException
	 */
	public void load() throws IOException {
		if (!deobfMapFile.exists()) {
			return;
		}
		List<String> lines = FileUtils.readLines(deobfMapFile, MAP_FILE_CHARSET);
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
					if (preLoadClsMap.isEmpty()) {
						preLoadClsMap = new HashMap<String, String>();
					}
					preLoadClsMap.put(va[0], va[1]);
				}
			}
		}
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
	 */
	public void save() throws IOException {
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
		for (DeobfClsInfo deobfClsInfo : clsMap.values()) {
			if (deobfClsInfo.alias != null) {
				list.add(String.format("c %s=%s", deobfClsInfo.cls.getFullName(), deobfClsInfo.alias));
			}
		}
		Collections.sort(list);
		FileUtils.writeLines(deobfMapFile, MAP_FILE_CHARSET, list);
		list.clear();
	}

	public String getPackageName(String packageName) {
		final PackageNode pkg = getPackageNode(packageName, false);
		if (pkg != null) {
			return pkg.getFullAlias();
		}
		return packageName;
	}

	public String getClassName(ClassInfo clsInfo) {
		final DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.makeNameWithoutPkg();
		}
		return getNameWithoutPackage(clsInfo);
	}

	public String getClassFullName(ClassNode cls) {
		return getClassFullName(cls.getClassInfo());
	}

	public String getClassFullName(ClassInfo clsInfo) {
		final DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.getFullName();
		}
		return getPackageName(clsInfo.getPackage()) + classNameSeparator + getClassName(clsInfo);
	}
}
