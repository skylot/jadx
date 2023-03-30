package jadx.core.dex.visitors.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.deobf.IAliasProvider;
import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;

public class RenameVisitor extends AbstractVisitor {
	private static final Pattern ANONYMOUS_CLASS_PATTERN = Pattern.compile("^\\d+$");

	@Override
	public void init(RootNode root) {
		List<File> inputFiles = root.getArgs().getInputFiles();
		if (inputFiles.isEmpty()) {
			return;
		}
		process(root);
		root.registerCodeDataUpdateListener(codeData -> process(root));
	}

	private void process(RootNode root) {
		KotlinMetadataRename.process(root);
		SourceFileRename.process(root);

		UserRenames.apply(root);
		checkNames(root);
	}

	private static void checkNames(RootNode root) {
		JadxArgs args = root.getArgs();
		if (args.getRenameFlags().isEmpty()) {
			return;
		}

		IAliasProvider aliasProvider = args.getAliasProvider();

		List<ClassNode> classes = root.getClasses(true);
		for (ClassNode cls : classes) {
			checkClassName(aliasProvider, cls, args);
			checkFields(aliasProvider, cls, args);
			checkMethods(aliasProvider, cls, args);
		}
		if (!args.isFsCaseSensitive() && args.isRenameCaseSensitive()) {
			Set<String> clsFullPaths = new HashSet<>(classes.size());
			for (ClassNode cls : classes) {
				ClassInfo clsInfo = cls.getClassInfo();
				if (!clsFullPaths.add(clsInfo.getAliasFullPath().toLowerCase())) {
					clsInfo.changeShortName(aliasProvider.forClass(cls));
					cls.addAttr(new RenameReasonAttr(cls).append("case insensitive filesystem"));
					clsFullPaths.add(clsInfo.getAliasFullPath().toLowerCase());
				}
			}
		}
		boolean pkgUpdated = false;
		for (PackageNode pkg : root.getPackages()) {
			pkgUpdated |= checkPackage(args, aliasProvider, pkg);
		}
		if (pkgUpdated) {
			root.runPackagesUpdate();
		}
		processRootPackages(aliasProvider, root, classes);
	}

	private static void checkClassName(IAliasProvider aliasProvider, ClassNode cls, JadxArgs args) {
		ClassInfo classInfo = cls.getClassInfo();
		String clsName = classInfo.getAliasShortName();

		String newShortName = fixClsShortName(args, clsName);
		if (newShortName == null) {
			// rename failed, use deobfuscator
			cls.rename(aliasProvider.forClass(cls));
			cls.addAttr(new RenameReasonAttr(cls).notPrintable());
			return;
		}
		if (!newShortName.equals(clsName)) {
			classInfo.changeShortName(newShortName);
			cls.addAttr(new RenameReasonAttr(cls).append("invalid class name"));
		}
		if (classInfo.isInner() && args.isRenameValid()) {
			// check inner classes names
			ClassInfo parentClass = classInfo.getParentClass();
			while (parentClass != null) {
				if (parentClass.getAliasShortName().equals(newShortName)) {
					cls.rename(aliasProvider.forClass(cls));
					cls.addAttr(new RenameReasonAttr(cls).append("collision with other inner class name"));
					break;
				}
				parentClass = parentClass.getParentClass();
			}
		}
	}

	private static boolean checkPackage(JadxArgs args, IAliasProvider aliasProvider, PackageNode pkg) {
		if (args.isRenameValid() && pkg.getAliasPkgInfo().isDefaultPkg()) {
			pkg.setFullAlias(Consts.DEFAULT_PACKAGE_NAME, false);
			return true;
		}
		String pkgName = pkg.getAliasPkgInfo().getName();
		boolean notValid = args.isRenameValid() && !NameMapper.isValidIdentifier(pkgName);
		boolean notPrintable = args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(pkgName);
		if (notValid || notPrintable) {
			pkg.setLeafAlias(aliasProvider.forPackage(pkg), false);
			return true;
		}
		return false;
	}

	@Nullable
	private static String fixClsShortName(JadxArgs args, String clsName) {
		boolean renameValid = args.isRenameValid();
		if (renameValid) {
			if (ANONYMOUS_CLASS_PATTERN.matcher(clsName).matches()) {
				return Consts.ANONYMOUS_CLASS_PREFIX + NameMapper.removeInvalidCharsMiddle(clsName);
			}

			char firstChar = clsName.charAt(0);
			if (firstChar == '$' || Character.isDigit(firstChar)) {
				return 'C' + NameMapper.removeInvalidCharsMiddle(clsName);
			}
		}
		String cleanClsName = args.isRenamePrintable()
				? NameMapper.removeNonPrintableCharacters(clsName)
				: clsName;
		if (cleanClsName.isEmpty()) {
			return null;
		}
		if (renameValid) {
			cleanClsName = NameMapper.removeInvalidChars(clsName, "C");
			if (!NameMapper.isValidIdentifier(cleanClsName)) {
				return 'C' + cleanClsName;
			}
		}
		return cleanClsName;
	}

	private static void checkFields(IAliasProvider aliasProvider, ClassNode cls, JadxArgs args) {
		Set<String> names = new HashSet<>();
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			String fieldName = fieldInfo.getAlias();
			boolean notUnique = !names.add(fieldName);
			boolean notValid = args.isRenameValid() && !NameMapper.isValidIdentifier(fieldName);
			boolean notPrintable = args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(fieldName);
			if (notUnique || notValid || notPrintable) {
				field.rename(aliasProvider.forField(field));
				field.addAttr(new RenameReasonAttr(field, notValid, notPrintable));
				if (notUnique) {
					field.addAttr(new RenameReasonAttr(field).append("collision with other field name"));
				}
			}
		}
	}

	private static void checkMethods(IAliasProvider aliasProvider, ClassNode cls, JadxArgs args) {
		List<MethodNode> methods = new ArrayList<>(cls.getMethods().size());
		for (MethodNode method : cls.getMethods()) {
			if (!method.getAccessFlags().isConstructor()) {
				methods.add(method);
			}
		}
		for (MethodNode mth : methods) {
			String alias = mth.getAlias();
			boolean notValid = args.isRenameValid() && !NameMapper.isValidIdentifier(alias);
			boolean notPrintable = args.isRenamePrintable() && !NameMapper.isAllCharsPrintable(alias);
			if (notValid || notPrintable) {
				mth.rename(aliasProvider.forMethod(mth));
				mth.addAttr(new RenameReasonAttr(mth, notValid, notPrintable));
			}
		}
		// Rename methods with same signature
		if (args.isRenameValid()) {
			Set<String> names = new HashSet<>(methods.size());
			for (MethodNode mth : methods) {
				String signature = mth.getMethodInfo().makeSignature(true, false);
				if (!names.add(signature) && canRename(mth)) {
					mth.rename(aliasProvider.forMethod(mth));
					mth.addAttr(new RenameReasonAttr("collision with other method in class"));
				}
			}
		}
	}

	private static boolean canRename(MethodNode mth) {
		if (mth.contains(AFlag.DONT_RENAME)) {
			return false;
		}
		MethodOverrideAttr overrideAttr = mth.get(AType.METHOD_OVERRIDE);
		if (overrideAttr != null) {
			for (MethodNode relatedMth : overrideAttr.getRelatedMthNodes()) {
				if (relatedMth != mth && mth.getParentClass().equals(relatedMth.getParentClass())) {
					// ignore rename if exists related method from same class (bridge method in most cases)
					// such rename will also rename current method and will not help to resolve name collision
					return false;
				}
			}
		}
		return true;
	}

	private static void processRootPackages(IAliasProvider aliasProvider, RootNode root, List<ClassNode> classes) {
		Set<String> rootPkgs = collectRootPkgs(root);
		root.getCacheStorage().setRootPkgs(rootPkgs);

		if (root.getArgs().isRenameValid()) {
			// rename field if collide with any root package
			for (ClassNode cls : classes) {
				for (FieldNode field : cls.getFields()) {
					if (rootPkgs.contains(field.getAlias())) {
						field.rename(aliasProvider.forField(field));
						field.addAttr(new RenameReasonAttr("collision with root package name"));
					}
				}
			}
		}
	}

	private static Set<String> collectRootPkgs(RootNode root) {
		Set<String> rootPkgs = new HashSet<>();
		for (PackageNode pkg : root.getPackages()) {
			if (pkg.isRoot()) {
				rootPkgs.add(pkg.getPkgInfo().getName());
			}
		}
		return rootPkgs;
	}

	@Override
	public String getName() {
		return "RenameVisitor";
	}
}
