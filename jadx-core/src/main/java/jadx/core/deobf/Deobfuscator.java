package jadx.core.deobf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.kotlin.KotlinMetadataUtils;

public class Deobfuscator {
	private static final Logger LOG = LoggerFactory.getLogger(Deobfuscator.class);

	private static final boolean DEBUG = false;

	public static final String CLASS_NAME_SEPARATOR = ".";
	public static final String INNER_CLASS_SEPARATOR = "$";

	private final JadxArgs args;
	private final RootNode root;
	private final DeobfPresets deobfPresets;

	private final Map<ClassInfo, DeobfClsInfo> clsMap = new LinkedHashMap<>();
	private final Map<FieldInfo, String> fldMap = new HashMap<>();
	private final Map<MethodInfo, String> mthMap = new HashMap<>();

	private final PackageNode rootPackage = new PackageNode("");
	private final Set<String> pkgSet = new TreeSet<>();
	private final Set<String> reservedClsNames = new HashSet<>();

	private final NavigableSet<MethodNode> mthProcessQueue = new TreeSet<>();

	private final int maxLength;
	private final int minLength;
	private final boolean useSourceNameAsAlias;
	private final boolean parseKotlinMetadata;

	private int pkgIndex = 0;
	private int clsIndex = 0;
	private int fldIndex = 0;
	private int mthIndex = 0;

	public Deobfuscator(RootNode root) {
		this.root = root;
		this.args = root.getArgs();

		this.minLength = args.getDeobfuscationMinLength();
		this.maxLength = args.getDeobfuscationMaxLength();
		this.useSourceNameAsAlias = args.isUseSourceNameAsClassAlias();
		this.parseKotlinMetadata = args.isParseKotlinMetadata();

		this.deobfPresets = DeobfPresets.build(root);
	}

	public void execute() {
		if (!args.isDeobfuscationForceSave()) {
			deobfPresets.load();
			for (Map.Entry<String, String> pkgEntry : deobfPresets.getPkgPresetMap().entrySet()) {
				addPackagePreset(pkgEntry.getKey(), pkgEntry.getValue());
			}
			deobfPresets.getPkgPresetMap().clear(); // not needed anymore
			initIndexes();
		}
		process();
	}

	public void savePresets() {
		Path deobfMapFile = deobfPresets.getDeobfMapFile();
		if (Files.exists(deobfMapFile) && !args.isDeobfuscationForceSave()) {
			LOG.warn("Deobfuscation map file '{}' exists. Use command line option '--deobf-rewrite-cfg' to rewrite it",
					deobfMapFile.toAbsolutePath());
			return;
		}
		try {
			deobfPresets.clear();
			fillDeobfPresets();
			deobfPresets.save();
		} catch (Exception e) {
			LOG.error("Failed to save deobfuscation map file '{}'", deobfMapFile.toAbsolutePath(), e);
		}
	}

	private void fillDeobfPresets() {
		for (PackageNode p : getRootPackage().getInnerPackages()) {
			for (PackageNode pp : p.getInnerPackages()) {
				dfsPackageName(p.getName(), pp);
			}
			if (p.hasAlias()) {
				deobfPresets.getPkgPresetMap().put(p.getName(), p.getAlias());
			}
		}
		for (DeobfClsInfo deobfClsInfo : clsMap.values()) {
			if (deobfClsInfo.getAlias() != null) {
				deobfPresets.getClsPresetMap().put(deobfClsInfo.getCls().getClassInfo().makeRawFullName(), deobfClsInfo.getAlias());
			}
		}
		for (FieldInfo fld : fldMap.keySet()) {
			deobfPresets.getFldPresetMap().put(fld.getRawFullId(), fld.getAlias());
		}
		for (MethodInfo mth : mthMap.keySet()) {
			deobfPresets.getMthPresetMap().put(mth.getRawFullId(), mth.getAlias());
		}
	}

	private void dfsPackageName(String prefix, PackageNode node) {
		for (PackageNode pp : node.getInnerPackages()) {
			dfsPackageName(prefix + '.' + node.getName(), pp);
		}
		if (node.hasAlias()) {
			deobfPresets.getPkgPresetMap().put(node.getName(), node.getAlias());
		}
	}

	public void clear() {
		deobfPresets.clear();
		clsMap.clear();
		fldMap.clear();
		mthMap.clear();
	}

	private void initIndexes() {
		pkgIndex = pkgSet.size();
		clsIndex = deobfPresets.getClsPresetMap().size();
		fldIndex = deobfPresets.getFldPresetMap().size();
		mthIndex = deobfPresets.getMthPresetMap().size();
	}

	private void preProcess() {
		for (ClassNode cls : root.getClasses()) {
			Collections.addAll(reservedClsNames, cls.getPackage().split("\\."));
		}
		for (ClassNode cls : root.getClasses()) {
			preProcessClass(cls);
		}
	}

	private void process() {
		preProcess();
		if (DEBUG) {
			dumpAlias();
		}
		for (ClassNode cls : root.getClasses()) {
			processClass(cls);
		}
		while (true) {
			MethodNode next = mthProcessQueue.pollLast();
			if (next == null) {
				break;
			}
			renameMethod(next);
		}
	}

	private void processClass(ClassNode cls) {
		if (isR(cls.getParentClass())) {
			return;
		}
		ClassInfo clsInfo = cls.getClassInfo();
		DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			clsInfo.changeShortName(deobfClsInfo.getAlias());
			PackageNode pkgNode = deobfClsInfo.getPkg();
			if (!clsInfo.isInner() && pkgNode.hasAnyAlias()) {
				clsInfo.changePkg(pkgNode.getFullAlias());
			}
		} else if (!clsInfo.isInner()) {
			// check if package renamed
			PackageNode pkgNode = getPackageNode(clsInfo.getPackage(), false);
			if (pkgNode != null && pkgNode.hasAnyAlias()) {
				clsInfo.changePkg(pkgNode.getFullAlias());
			}
		}
		for (FieldNode field : cls.getFields()) {
			if (field.contains(AFlag.DONT_RENAME)) {
				continue;
			}
			renameField(field);
		}
		mthProcessQueue.addAll(cls.getMethods());

		for (ClassNode innerCls : cls.getInnerClasses()) {
			processClass(innerCls);
		}
	}

	private void renameField(FieldNode field) {
		FieldInfo fieldInfo = field.getFieldInfo();
		String alias = getFieldAlias(field);
		if (alias != null) {
			fieldInfo.setAlias(alias);
		}
	}

	public void forceRenameField(FieldNode field) {
		field.getFieldInfo().setAlias(makeFieldAlias(field));
	}

	private void renameMethod(MethodNode mth) {
		MethodInfo mthInfo = mth.getMethodInfo();
		Set<String> names = deobfPresets.getForVars(mthInfo);
		if (names != null) {
			mthInfo.setVarNameMap(names);
		}
		String alias = getMethodAlias(mth);
		if (alias != null) {
			applyMethodAlias(mth, alias);
		}
	}

	public void forceRenameMethod(MethodNode mth) {
		String alias = makeMethodAlias(mth);
		applyMethodAlias(mth, alias);
	}

	private void applyMethodAlias(MethodNode mth, String alias) {
		setSingleMethodAlias(mth, alias);

		MethodOverrideAttr overrideAttr = mth.get(AType.METHOD_OVERRIDE);
		if (overrideAttr != null) {
			for (MethodNode ovrdMth : overrideAttr.getRelatedMthNodes()) {
				if (ovrdMth != mth) {
					setSingleMethodAlias(ovrdMth, alias);
				}
			}
		}
	}

	private void setSingleMethodAlias(MethodNode mth, String alias) {
		MethodInfo mthInfo = mth.getMethodInfo();
		mthInfo.setAlias(alias);
		mthMap.put(mthInfo, alias);
		mthProcessQueue.remove(mth);
	}

	public void addPackagePreset(String origPkgName, String pkgAlias) {
		PackageNode pkg = getPackageNode(origPkgName, true);
		pkg.setAlias(pkgAlias);
	}

	/**
	 * Gets package node for full package name
	 *
	 * @param fullPkgName full package name
	 * @param create      if {@code true} then will create all absent objects
	 * @return package node object or {@code null} if no package found and <b>create</b> set to
	 *         {@code false}
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

	String getNameWithoutPackage(ClassInfo clsInfo) {
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

	private void preProcessClass(ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		String pkgFullName = classInfo.getPackage();
		PackageNode pkg = getPackageNode(pkgFullName, true);
		processPackageFull(pkg, pkgFullName);

		String alias = deobfPresets.getForCls(classInfo);
		if (alias != null) {
			clsMap.put(classInfo, new DeobfClsInfo(this, cls, pkg, alias));
		} else {
			if (!clsMap.containsKey(classInfo)) {
				String clsShortName = classInfo.getShortName();
				boolean badName = shouldRename(clsShortName)
						|| (args.isRenameValid() && reservedClsNames.contains(clsShortName));
				makeClsAlias(cls, badName);
			}
		}
		for (ClassNode innerCls : cls.getInnerClasses()) {
			preProcessClass(innerCls);
		}
	}

	public String getClsAlias(ClassNode cls) {
		DeobfClsInfo deobfClsInfo = clsMap.get(cls.getClassInfo());
		if (deobfClsInfo != null) {
			return deobfClsInfo.getAlias();
		}
		return makeClsAlias(cls, true);
	}

	public String getPkgAlias(ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		PackageNode pkg;
		DeobfClsInfo deobfClsInfo = clsMap.get(classInfo);
		if (deobfClsInfo != null) {
			pkg = deobfClsInfo.getPkg();
		} else {
			String fullPkgName = classInfo.getPackage();
			pkg = getPackageNode(fullPkgName, true);
			processPackageFull(pkg, fullPkgName);
		}
		if (pkg.hasAnyAlias()) {
			return pkg.getFullAlias();
		} else {
			return pkg.getFullName();
		}
	}

	private String makeClsAlias(ClassNode cls, boolean badName) {
		String alias = null;
		String pkgName = null;
		if (this.parseKotlinMetadata) {
			ClassInfo kotlinCls = KotlinMetadataUtils.getClassName(cls);
			if (kotlinCls != null) {
				alias = prepareNameFull(kotlinCls.getShortName(), "C");
				pkgName = kotlinCls.getPackage();
			}
		}
		if (alias == null && this.useSourceNameAsAlias) {
			alias = getAliasFromSourceFile(cls);
		}

		ClassInfo classInfo = cls.getClassInfo();
		if (alias == null) {
			if (badName) {
				String clsName = classInfo.getShortName();
				String prefix = makeClsPrefix(cls);
				alias = String.format("%sC%04d%s", prefix, clsIndex++, prepareNamePart(clsName));
			} else {
				// rename not needed
				return classInfo.getShortName();
			}
		}
		if (pkgName == null) {
			pkgName = classInfo.getPackage();
		}
		PackageNode pkg = getPackageNode(pkgName, true);
		clsMap.put(classInfo, new DeobfClsInfo(this, cls, pkg, alias));
		return alias;
	}

	/**
	 * Generate a prefix for a class name that bases on certain class properties, certain
	 * extended superclasses or implemented interfaces.
	 */
	private String makeClsPrefix(ClassNode cls) {
		if (cls.isEnum()) {
			return "Enum";
		}
		String result = "";
		if (cls.getAccessFlags().isAbstract()) {
			result += "Abstract";
		}

		// Process current class and all super classes
		ClassNode currentCls = cls;
		outerLoop: while (currentCls != null) {
			if (currentCls.getSuperClass() != null) {
				String superClsName = currentCls.getSuperClass().getObject();
				if (superClsName.startsWith("android.app.")) {
					// e.g. Activity or Fragment
					result += superClsName.substring(12);
					break;
				} else if (superClsName.startsWith("android.os.")) {
					// e.g. AsyncTask
					result += superClsName.substring(11);
					break;
				}
			}
			for (ArgType intf : cls.getInterfaces()) {
				String intfClsName = intf.getObject();
				if (intfClsName.equals("java.lang.Runnable")) {
					result += "Runnable";
					break outerLoop;
				} else if (intfClsName.startsWith("java.util.concurrent.")) {
					// e.g. Callable
					result += intfClsName.substring(21);
					break outerLoop;
				} else if (intfClsName.startsWith("android.view.")) {
					// e.g. View.OnClickListener
					result += intfClsName.substring(13);
					break outerLoop;
				} else if (intfClsName.startsWith("android.content.")) {
					// e.g. DialogInterface.OnClickListener
					result += intfClsName.substring(16);
					break outerLoop;
				}
			}
			if (currentCls.getSuperClass() == null) {
				break;
			}
			currentCls = cls.root().resolveClass(currentCls.getSuperClass());
		}
		return result;
	}

	@Nullable
	private String getAliasFromSourceFile(ClassNode cls) {
		SourceFileAttr sourceFileAttr = cls.get(AType.SOURCE_FILE);
		if (sourceFileAttr == null) {
			return null;
		}
		if (cls.getClassInfo().isInner()) {
			return null;
		}
		String name = sourceFileAttr.getFileName();
		if (name.endsWith(".java")) {
			name = name.substring(0, name.length() - ".java".length());
		} else if (name.endsWith(".kt")) {
			name = name.substring(0, name.length() - ".kt".length());
		}
		if (!NameMapper.isValidAndPrintable(name)) {
			return null;
		}
		for (DeobfClsInfo deobfClsInfo : clsMap.values()) {
			if (deobfClsInfo.getAlias().equals(name)) {
				return null;
			}
		}
		ClassNode otherCls = cls.root().resolveClass(cls.getPackage() + '.' + name);
		if (otherCls != null) {
			return null;
		}
		cls.remove(AType.SOURCE_FILE);
		return name;
	}

	@Nullable
	private String getFieldAlias(FieldNode field) {
		FieldInfo fieldInfo = field.getFieldInfo();
		String alias = fldMap.get(fieldInfo);
		if (alias != null) {
			return alias;
		}
		alias = deobfPresets.getForFld(fieldInfo);
		if (alias != null) {
			fldMap.put(fieldInfo, alias);
			return alias;
		}
		if (shouldRename(field.getName())) {
			return makeFieldAlias(field);
		}
		return null;
	}

	@Nullable
	private String getMethodAlias(MethodNode mth) {
		if (mth.contains(AFlag.DONT_RENAME)) {
			return null;
		}
		MethodInfo methodInfo = mth.getMethodInfo();
		if (methodInfo.isClassInit() || methodInfo.isConstructor()) {
			return null;
		}
		String alias = getAssignedAlias(methodInfo);
		if (alias != null) {
			return alias;
		}
		if (shouldRename(mth.getName())) {
			return makeMethodAlias(mth);
		}
		return null;
	}

	@Nullable
	private String getAssignedAlias(MethodInfo methodInfo) {
		String alias = mthMap.get(methodInfo);
		if (alias != null) {
			return alias;
		}
		return deobfPresets.getForMth(methodInfo);
	}

	public String makeFieldAlias(FieldNode field) {
		String alias = String.format("f%d%s", fldIndex++, prepareNamePart(field.getName()));
		fldMap.put(field.getFieldInfo(), alias);
		return alias;
	}

	public String makeMethodAlias(MethodNode mth) {
		String prefix;
		if (mth.contains(AType.METHOD_OVERRIDE)) {
			prefix = "mo";
		} else {
			prefix = "m";
		}
		return String.format("%s%d%s", prefix, mthIndex++, prepareNamePart(mth.getName()));
	}

	private void processPackageFull(PackageNode pkg, String fullName) {
		if (pkgSet.contains(fullName)) {
			return;
		}
		pkgSet.add(fullName);

		// doPkg for all parent packages except root that not hasAliases
		PackageNode parentPkg = pkg.getParentPackage();
		while (!parentPkg.getName().isEmpty()) {
			if (!parentPkg.hasAlias()) {
				processPackageFull(parentPkg, parentPkg.getFullName());
			}
			parentPkg = parentPkg.getParentPackage();
		}

		if (!pkg.hasAlias()) {
			String pkgName = pkg.getName();
			if ((args.isDeobfuscationOn() && shouldRename(pkgName))
					|| (args.isRenameValid() && !NameMapper.isValidIdentifier(pkgName))
					|| (args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(pkgName))) {
				String pkgAlias = String.format("p%03d%s", pkgIndex++, prepareNamePart(pkg.getName()));
				pkg.setAlias(pkgAlias);
			}
		}
	}

	private boolean shouldRename(String s) {
		int len = s.length();
		return len < minLength || len > maxLength;
	}

	private String prepareNamePart(String name) {
		if (name.length() > maxLength) {
			return 'x' + Integer.toHexString(name.hashCode());
		}
		return NameMapper.removeInvalidCharsMiddle(name);
	}

	private String prepareNameFull(String name, String prefix) {
		if (name.length() > maxLength) {
			return makeHashName(name, prefix);
		}
		String result = NameMapper.removeInvalidChars(name, prefix);
		if (result.isEmpty()) {
			return makeHashName(name, prefix);
		}
		if (NameMapper.isReserved(result)) {
			return prefix + result;
		}
		return result;
	}

	private static String makeHashName(String name, String invalidPrefix) {
		return invalidPrefix + 'x' + Integer.toHexString(name.hashCode());
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
		for (ClassNode cls : root.getClasses()) {
			dumpClassAlias(cls);
		}
	}

	private String getPackageName(String packageName) {
		PackageNode pkg = getPackageNode(packageName, false);
		if (pkg != null) {
			return pkg.getFullAlias();
		}
		return packageName;
	}

	private String getClassName(ClassInfo clsInfo) {
		DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.makeNameWithoutPkg();
		}
		return getNameWithoutPackage(clsInfo);
	}

	private String getClassFullName(ClassNode cls) {
		ClassInfo clsInfo = cls.getClassInfo();
		DeobfClsInfo deobfClsInfo = clsMap.get(clsInfo);
		if (deobfClsInfo != null) {
			return deobfClsInfo.getFullName();
		}
		return getPackageName(clsInfo.getPackage()) + CLASS_NAME_SEPARATOR + getClassName(clsInfo);
	}

	public Map<ClassInfo, DeobfClsInfo> getClsMap() {
		return clsMap;
	}

	public Map<FieldInfo, String> getFldMap() {
		return fldMap;
	}

	public Map<MethodInfo, String> getMthMap() {
		return mthMap;
	}

	public PackageNode getRootPackage() {
		return rootPackage;
	}

	private static boolean isR(ClassNode cls) {
		if (!cls.getClassInfo().getShortName().equals("R")) {
			return false;
		}
		if (!cls.getMethods().isEmpty() || !cls.getFields().isEmpty()) {
			return false;
		}
		for (ClassNode inner : cls.getInnerClasses()) {
			for (MethodNode m : inner.getMethods()) {
				if (!m.getMethodInfo().isConstructor() && !m.getMethodInfo().isClassInit()) {
					return false;
				}
			}
			for (FieldNode field : cls.getFields()) {
				ArgType type = field.getType();
				if (type != ArgType.INT && (!type.isArray() || type.getArrayElement() != ArgType.INT)) {
					return false;
				}
			}
		}
		return true;
	}
}
