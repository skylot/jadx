package jadx.core.deobf;

import jadx.api.IJadxArgs;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deobfuscator {
	private static final Logger LOG = LoggerFactory.getLogger(Deobfuscator.class);

	private static final boolean DEBUG = false;

	private static final String MAP_FILE_CHARSET = "UTF-8";
	private static final String CLASS_NAME_SEPARATOR = ".";
	private static final String INNER_CLASS_SEPARATOR = "$";

	private final Map<ClassInfo, DeobfClsInfo> clsMap = new HashMap<ClassInfo, DeobfClsInfo>();
	private final IJadxArgs args;
	private final File deobfMapFile;
	@NotNull
	private final List<DexNode> dexNodes;

	private final int maxLength;
	private final int minLength;
	private int pkgIndex = 0;
	private int clsIndex = 0;

	private final PackageNode rootPackage = new PackageNode("");
	private final Set<String> pkgSet = new TreeSet<String>();
	private Map<String, String> preLoadClsMap = Collections.emptyMap();

	public Deobfuscator(IJadxArgs args, @NotNull List<DexNode> dexNodes, File deobfMapFile) {
		this.args = args;
		this.dexNodes = dexNodes;
		this.deobfMapFile = deobfMapFile;

		this.minLength = args.getDeobfuscationMinLength();
		this.maxLength = args.getDeobfuscationMaxLength();
	}

	public void execute() {
		if (deobfMapFile.exists() && !args.isDeobfuscationForceSave()) {
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

	private void process() {
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
				if (!fullName.equals(clsInfo.getFullName())) {
					clsInfo.rename(dexNode, fullName);
				}
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
	private PackageNode getPackageNode(String fullPkgName, boolean create) {
		if (fullPkgName.isEmpty() || fullPkgName.equals(CLASS_NAME_SEPARATOR)) {
			return rootPackage;
		}
		PackageNode result = rootPackage;
		PackageNode parentNode;
		do {
			String pkgName;
			int idx = fullPkgName.indexOf(CLASS_NAME_SEPARATOR);

			if (idx > -1) {
				pkgName = fullPkgName.substring(0, idx);
				fullPkgName = fullPkgName.substring(idx + 1);
			} else {
				pkgName = fullPkgName;
				fullPkgName = "";
			}
			parentNode = result;
			result = result.getInnerPackageByName(pkgName);
			if (result == null && create) {
				result = new PackageNode(pkgName);
				parentNode.addInnerPackage(result);
			}
		} while (!fullPkgName.isEmpty() && result != null);

		return result;
	}

	private final class DeobfClsInfo {
		public final ClassNode cls;
		public final PackageNode pkg;
		public final String alias;

		public DeobfClsInfo(ClassNode cls, PackageNode pkg, String alias) {
			this.cls = cls;
			this.pkg = pkg;
			this.alias = alias;
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
				prefix += INNER_CLASS_SEPARATOR;
			} else {
				prefix = "";
			}

			return prefix + (this.alias != null ? this.alias : this.cls.getShortName());
		}

		public String getFullName() {
			return pkg.getFullAlias() + CLASS_NAME_SEPARATOR + makeNameWithoutPkg();
		}
	}

	private String getNameWithoutPackage(ClassInfo clsInfo) {
		String prefix;
		ClassInfo parentClsInfo = clsInfo.getParentClass();
		if (parentClsInfo != null) {
			DeobfClsInfo parentDeobfClsInfo = clsMap.get(parentClsInfo);
			if (parentDeobfClsInfo != null) {
				prefix = parentDeobfClsInfo.makeNameWithoutPkg();
			} else {
				prefix = getNameWithoutPackage(parentClsInfo);
			}
			prefix += INNER_CLASS_SEPARATOR;
		} else {
			prefix = "";
		}
		return prefix + clsInfo.getShortName();
	}

	private void doClass(ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		String pkgFullName = classInfo.getPackage();
		PackageNode pkg = getPackageNode(pkgFullName, true);
		doPkg(pkg, pkgFullName);

		String fullName = classInfo.getFullName();
		if (preLoadClsMap.containsKey(fullName)) {
			String alias = preLoadClsMap.get(fullName);
			clsMap.put(classInfo, new DeobfClsInfo(cls, pkg, alias));
			return;
		}
		if (clsMap.containsKey(classInfo)) {
			return;
		}
		if (shouldRename(classInfo.getShortName())) {
			String alias = makeClsAlias(cls);
			clsMap.put(classInfo, new DeobfClsInfo(cls, pkg, alias));
		}
	}

	private String makeClsAlias(ClassNode cls) {
		SourceFileAttr sourceFileAttr = cls.get(AType.SOURCE_FILE);
		if (sourceFileAttr != null) {
			String name = sourceFileAttr.getFileName();
			if (name.endsWith(".java")) {
				name = name.substring(0, name.length() - ".java".length());
			}
			if (NameMapper.isValidIdentifier(name)
					&& !NameMapper.isReserved(name)) {
				// TODO: check if no class with this name exists or already renamed
				cls.remove(AType.SOURCE_FILE);
				return name;
			}
		}
		String clsName = cls.getClassInfo().getShortName();
		return String.format("C%04d%s", clsIndex++, makeName(clsName));
	}

	private void doPkg(PackageNode pkg, String fullName) {
		if (pkgSet.contains(fullName)) {
			return;
		}
		pkgSet.add(fullName);

		// doPkg for all parent packages except root that not hasAliases
		PackageNode parentPkg = pkg.getParentPackage();
		while (!parentPkg.getName().isEmpty()) {
			if (!parentPkg.hasAlias()) {
				doPkg(parentPkg, parentPkg.getFullName());
			}
			parentPkg = parentPkg.getParentPackage();
		}

		final String pkgName = pkg.getName();
		if (!pkg.hasAlias() && shouldRename(pkgName)) {
			final String pkgAlias = String.format("p%03d%s", pkgIndex++, makeName(pkgName));
			pkg.setAlias(pkgAlias);
		}
	}

	private void preProcess() {
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				doClass(cls);
			}
		}
	}

	private boolean shouldRename(String s) {
		return s.length() > maxLength
				|| s.length() < minLength
				|| NameMapper.isReserved(s)
				|| !NameMapper.isAllCharsPrintable(s);
	}

	private String makeName(String name) {
		if (name.length() > maxLength) {
			return "x" + Integer.toHexString(name.hashCode());
		}
		if (NameMapper.isReserved(name)) {
			return name;
		}
		if (!NameMapper.isAllCharsPrintable(name)) {
			return removeInvalidChars(name);
		}
		return name;
	}

	private String removeInvalidChars(String name) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			int ch = name.charAt(i);
			if (NameMapper.isPrintableChar(ch)) {
				sb.append((char) ch);
			}
		}
		return sb.toString();
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
	private void load() throws IOException {
		if (!deobfMapFile.exists()) {
			return;
		}
		LOG.info("Loading obfuscation map from: {}", deobfMapFile.getAbsoluteFile());
		List<String> lines = FileUtils.readLines(deobfMapFile, MAP_FILE_CHARSET);
		for (String l : lines) {
			l = l.trim();
			if (l.startsWith("p ")) {
				String[] va = splitAndTrim(l);
				if (va.length == 2) {
					PackageNode pkg = getPackageNode(va[0], true);
					pkg.setAlias(va[1]);
				}
			} else if (l.startsWith("c ")) {
				String[] va = splitAndTrim(l);
				if (va.length == 2) {
					if (preLoadClsMap.isEmpty()) {
						preLoadClsMap = new HashMap<String, String>();
					}
					preLoadClsMap.put(va[0], va[1]);
				}
			}
		}
	}

	private static String[] splitAndTrim(String str) {
		String[] v = str.substring(2).split("=");
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i].trim();
		}
		return v;
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
	private void save() throws IOException {
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
				list.add(String.format("c %s=%s",
						deobfClsInfo.cls.getClassInfo().getFullName(), deobfClsInfo.alias));
			}
		}
		Collections.sort(list);
		FileUtils.writeLines(deobfMapFile, MAP_FILE_CHARSET, list);
		list.clear();
	}

	private String getPackageName(String packageName) {
		final PackageNode pkg = getPackageNode(packageName, false);
		if (pkg != null) {
			return pkg.getFullAlias();
		}
		return packageName;
	}

	private String getClassName(ClassInfo clsInfo) {
		final DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.makeNameWithoutPkg();
		}
		return getNameWithoutPackage(clsInfo);
	}

	private String getClassFullName(ClassNode cls) {
		return getClassFullName(cls.getClassInfo());
	}

	private String getClassFullName(ClassInfo clsInfo) {
		final DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.getFullName();
		}
		return getPackageName(clsInfo.getPackage()) + CLASS_NAME_SEPARATOR + getClassName(clsInfo);
	}
}
