package jadx.core.deobf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.args.DeobfuscationMapFileMode;
import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.api.utils.CodeUtils;
import jadx.core.Consts;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
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
		if (args.getDeobfuscationMapFileMode().shouldRead()) {
			if (deobfPresets.load()) {
				for (Map.Entry<String, String> pkgEntry : deobfPresets.getPkgPresetMap().entrySet()) {
					addPackagePreset(pkgEntry.getKey(), pkgEntry.getValue());
				}
				deobfPresets.getPkgPresetMap().clear(); // not needed anymore
				initIndexes();
			}
		}
		process();
	}

	public void savePresets() {
		DeobfuscationMapFileMode mode = args.getDeobfuscationMapFileMode();
		if (!mode.shouldWrite()) {
			return;
		}
		Path deobfMapFile = deobfPresets.getDeobfMapFile();
		if (mode == DeobfuscationMapFileMode.READ_OR_SAVE && Files.exists(deobfMapFile)) {
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

	private List<VarNode> collectMethodArgs(MethodNode methodNode) {
		ICodeInfo codeInfo = methodNode.getTopParentClass().getCode();
		int mthDefPos = methodNode.getDefPosition();
		int lineEndPos = CodeUtils.getLineEndForPos(codeInfo.getCodeStr(), mthDefPos);
		List<VarNode> args = new ArrayList<>();
		codeInfo.getCodeMetadata().searchDown(mthDefPos, (pos, ann) -> {
			if (pos > lineEndPos) {
				// Stop at line end
				return Boolean.TRUE;
			}
			if (ann instanceof NodeDeclareRef) {
				ICodeNodeRef declRef = ((NodeDeclareRef) ann).getNode();
				if (declRef instanceof VarNode) {
					VarNode varNode = (VarNode) declRef;
					if (!varNode.getMth().equals(methodNode)) {
						// Stop if we've gone too far and have entered a different method
						return Boolean.TRUE;
					}
					args.add(varNode);
				}
			}
			return null;
		});
		return args;
	}

	private List<SimpleEntry<VarNode, Integer>> collectMethodVars(MethodNode methodNode) {
		ICodeInfo codeInfo = methodNode.getTopParentClass().getCode();
		int mthDefPos = methodNode.getDefPosition();
		int mthLineEndPos = CodeUtils.getLineEndForPos(codeInfo.getCodeStr(), mthDefPos);

		List<SimpleEntry<VarNode, Integer>> vars = new ArrayList<>();
		AtomicInteger lastOffset = new AtomicInteger(-1);
		codeInfo.getCodeMetadata().searchDown(mthLineEndPos, (pos, ann) -> {
			if (ann instanceof InsnCodeOffset) {
				lastOffset.set(((InsnCodeOffset) ann).getOffset());
			}
			if (ann instanceof NodeDeclareRef) {
				ICodeNodeRef declRef = ((NodeDeclareRef) ann).getNode();
				if (declRef instanceof VarNode) {
					VarNode varNode = (VarNode) declRef;
					if (!varNode.getMth().equals(methodNode)) {
						// Stop if we've gone too far and have entered a different method
						return Boolean.TRUE;
					}
					if (lastOffset.get() != -1) {
						vars.add(new SimpleEntry<VarNode, Integer>(varNode, lastOffset.get()));
					} else {
						LOG.warn("Local variable not present in bytecode, skipping: "
								+ methodNode.getMethodInfo().getRawFullId() + "." + varNode.getName());
					}
					lastOffset.set(-1);
				}
			}
			return null;
		});
		return vars;
	}

	public void exportMappings(Path path, JadxCodeData codeData, MappingFormat mappingFormat) {
		MemoryMappingTree mappingTree = new MemoryMappingTree();
		// Map < SrcName >
		Set<String> mappedClasses = new HashSet<>();
		// Map < DeclClass + ShortId >
		Set<String> mappedFields = new HashSet<>();
		Set<String> mappedMethods = new HashSet<>();
		Set<String> methodsWithMappedElements = new HashSet<>();
		// Map < DeclClass + MethodShortId + CodeRef, NewName >
		Map<String, String> mappedMethodArgsAndVars = new HashMap<>();
		// Map < DeclClass + *ShortId + *CodeRef, Comment >
		Map<String, String> comments = new HashMap<>();

		// We have to do this so we know for sure which elements are *manually* renamed
		for (ICodeRename codeRename : codeData.getRenames()) {
			if (codeRename.getNodeRef().getType().equals(RefType.CLASS)) {
				mappedClasses.add(codeRename.getNodeRef().getDeclaringClass());
			} else if (codeRename.getNodeRef().getType().equals(RefType.FIELD)) {
				mappedFields.add(codeRename.getNodeRef().getDeclaringClass() + codeRename.getNodeRef().getShortId());
			} else if (codeRename.getNodeRef().getType().equals(RefType.METHOD)) {
				if (codeRename.getCodeRef() == null) {
					mappedMethods.add(codeRename.getNodeRef().getDeclaringClass() + codeRename.getNodeRef().getShortId());
				} else {
					methodsWithMappedElements.add(codeRename.getNodeRef().getDeclaringClass() + codeRename.getNodeRef().getShortId());
					mappedMethodArgsAndVars.put(codeRename.getNodeRef().getDeclaringClass()
							+ codeRename.getNodeRef().getShortId()
							+ codeRename.getCodeRef(),
							codeRename.getNewName());
				}
			}
		}
		for (ICodeComment codeComment : codeData.getComments()) {
			comments.put(codeComment.getNodeRef().getDeclaringClass()
					+ (codeComment.getNodeRef().getShortId() == null ? "" : codeComment.getNodeRef().getShortId())
					+ (codeComment.getCodeRef() == null ? "" : codeComment.getCodeRef()),
					codeComment.getComment());
			if (codeComment.getCodeRef() != null) {
				methodsWithMappedElements.add(codeComment.getNodeRef().getDeclaringClass() + codeComment.getNodeRef().getShortId());
			}
		}

		try {
			if (path.toFile().exists()) {
				path.toFile().delete();
			}
			path.toFile().createNewFile();

			mappingTree.visitHeader();
			mappingTree.visitNamespaces("official", Arrays.asList("named"));
			mappingTree.visitContent();

			for (ClassNode cls : root.getClasses()) {
				ClassInfo classInfo = cls.getClassInfo();
				String classPath = classInfo.makeRawFullName().replace('.', '/');
				String rawClassName = classInfo.getRawName();

				if (classInfo.hasAlias()
						&& !classInfo.getAliasShortName().equals(classInfo.getShortName())
						&& mappedClasses.contains(rawClassName)) {
					mappingTree.visitClass(classPath);
					String alias = classInfo.makeAliasRawFullName().replace('.', '/');

					if (alias.length() >= Consts.DEFAULT_PACKAGE_NAME.length()
							&& alias.substring(0, Consts.DEFAULT_PACKAGE_NAME.length()).equals(Consts.DEFAULT_PACKAGE_NAME)) {
						alias = alias.substring(Consts.DEFAULT_PACKAGE_NAME.length() + 1);
					}
					mappingTree.visitDstName(MappedElementKind.CLASS, 0, alias);
				}
				if (comments.containsKey(rawClassName)) {
					mappingTree.visitClass(classPath);
					mappingTree.visitComment(MappedElementKind.CLASS, comments.get(rawClassName));
				}

				for (FieldNode fld : cls.getFields()) {
					FieldInfo fieldInfo = fld.getFieldInfo();
					if (fieldInfo.hasAlias() && mappedFields.contains(rawClassName + fieldInfo.getShortId())) {
						visitField(mappingTree, classPath, fieldInfo.getName(), TypeGen.signature(fieldInfo.getType()));
						mappingTree.visitDstName(MappedElementKind.FIELD, 0, fieldInfo.getAlias());
					}
					if (comments.containsKey(rawClassName + fieldInfo.getShortId())) {
						visitField(mappingTree, classPath, fieldInfo.getName(), TypeGen.signature(fieldInfo.getType()));
						mappingTree.visitComment(MappedElementKind.FIELD, comments.get(rawClassName + fieldInfo.getShortId()));
					}
				}

				for (MethodNode mth : cls.getMethods()) {
					MethodInfo methodInfo = mth.getMethodInfo();
					String methodName = methodInfo.getName();
					String methodDesc = methodInfo.getShortId().substring(methodName.length());
					if (methodInfo.hasAlias() && mappedMethods.contains(rawClassName + methodInfo.getShortId())) {
						visitMethod(mappingTree, classPath, methodName, methodDesc);
						mappingTree.visitDstName(MappedElementKind.METHOD, 0, methodInfo.getAlias());
					}
					if (comments.containsKey(rawClassName + methodInfo.getShortId())) {
						visitMethod(mappingTree, classPath, methodName, methodDesc);
						mappingTree.visitComment(MappedElementKind.METHOD, comments.get(rawClassName + methodInfo.getShortId()));
					}

					if (!methodsWithMappedElements.contains(rawClassName + methodInfo.getShortId())) {
						continue;
					}
					// Method args
					List<VarNode> args = collectMethodArgs(mth);
					for (VarNode arg : args) {
						int lvIndex = arg.getReg() - args.get(0).getReg() + (mth.getAccessFlags().isStatic() ? 0 : 1);
						String key = rawClassName + methodInfo.getShortId()
								+ JadxCodeRef.forVar(arg.getReg(), arg.getSsa());
						if (mappedMethodArgsAndVars.containsKey(key)) {
							visitMethodArg(mappingTree, classPath, methodName, methodDesc, args.indexOf(arg), lvIndex);
							mappingTree.visitDstName(MappedElementKind.METHOD_ARG, 0, mappedMethodArgsAndVars.get(key));
							mappedMethodArgsAndVars.remove(key);
						}
						// Not checking for comments since method args can't have any
					}
					// Method vars
					List<SimpleEntry<VarNode, Integer>> vars = collectMethodVars(mth);
					for (SimpleEntry<VarNode, Integer> entry : vars) {
						VarNode var = entry.getKey();
						int offset = entry.getValue();
						int lvIndex = var.getReg() - vars.get(0).getKey().getReg() + (mth.getAccessFlags().isStatic() ? 0 : 1);
						String key = rawClassName + methodInfo.getShortId()
								+ JadxCodeRef.forVar(var.getReg(), var.getSsa());
						if (mappedMethodArgsAndVars.containsKey(key)) {
							visitMethodVar(mappingTree, classPath, methodName, methodDesc, lvIndex, var.getDefPosition());
							mappingTree.visitDstName(MappedElementKind.METHOD_VAR, 0, mappedMethodArgsAndVars.get(key));
						}
						key = rawClassName + methodInfo.getShortId()
								+ JadxCodeRef.forInsn(offset);
						if (comments.containsKey(key)) {
							visitMethodVar(mappingTree, classPath, methodName, methodDesc, lvIndex, var.getDefPosition());
							mappingTree.visitComment(MappedElementKind.METHOD_VAR, comments.get(key));
						}
					}
				}
			}

			MappingWriter writer = MappingWriter.create(path, mappingFormat);
			mappingTree.accept(writer);
			mappingTree.visitEnd();
			writer.close();
		} catch (IOException e) {
			LOG.error("Failed to save deobfuscation map file '{}'", path.toAbsolutePath(), e);
		}
	}

	private void visitField(MemoryMappingTree tree, String classPath, String srcName, String srcDesc) {
		tree.visitClass(classPath);
		tree.visitField(srcName, srcDesc);
	}

	private void visitMethod(MemoryMappingTree tree, String classPath, String srcName, String srcDesc) {
		tree.visitClass(classPath);
		tree.visitMethod(srcName, srcDesc);
	}

	private void visitMethodArg(MemoryMappingTree tree, String classPath, String methodSrcName, String methodSrcDesc, int argPosition,
			int lvIndex) {
		visitMethod(tree, classPath, methodSrcName, methodSrcDesc);
		tree.visitMethodArg(argPosition, lvIndex, null);
	}

	private void visitMethodVar(MemoryMappingTree tree, String classPath, String methodSrcName, String methodSrcDesc, int lvIndex,
			int startOpIdx) {
		visitMethod(tree, classPath, methodSrcName, methodSrcDesc);
		tree.visitMethodVar(-1, lvIndex, startOpIdx, null);
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
		for (ClassNode cls : root.getClasses()) {
			ClassInfo classInfo = cls.getClassInfo();
			if (classInfo.hasAlias()) {
				deobfPresets.getClsPresetMap().put(classInfo.makeRawFullName(), classInfo.getAliasShortName());
			}

			for (FieldNode fld : cls.getFields()) {
				FieldInfo fieldInfo = fld.getFieldInfo();
				if (fieldInfo.hasAlias()) {
					deobfPresets.getFldPresetMap().put(fieldInfo.getRawFullId(), fld.getAlias());
				}
			}

			for (MethodNode mth : cls.getMethods()) {
				MethodInfo methodInfo = mth.getMethodInfo();
				if (methodInfo.hasAlias()) {
					deobfPresets.getMthPresetMap().put(methodInfo.getRawFullId(), methodInfo.getAlias());
				}
			}
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
	 * @param fullPkgName
	 *                    full package name
	 * @param create
	 *                    if {@code true} then will create all absent objects
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
		if (classInfo.hasAliasPkg()) {
			// already renamed
			PackageNode pkg = getPackageNode(classInfo.getPackage(), true);
			// update all parts of package
			String[] aliasParts = classInfo.getAliasPkg().split("\\.");
			PackageNode subPkg = pkg;
			for (int i = aliasParts.length - 1; i >= 0; i--) {
				String aliasPart = aliasParts[i];
				if (!subPkg.getName().equals(aliasPart)) {
					subPkg.setAlias(aliasPart);
				}
				subPkg = subPkg.getParentPackage();
			}
			return pkg.getFullAlias();
		}
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
			ClsAliasPair kotlinCls = KotlinMetadataUtils.getClassAlias(cls);
			if (kotlinCls != null) {
				alias = kotlinCls.getName();
				pkgName = kotlinCls.getPkg();
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
		SourceFileAttr sourceFileAttr = cls.get(JadxAttrType.SOURCE_FILE);
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
		cls.remove(JadxAttrType.SOURCE_FILE);
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
					&& (pkg.getParentPackage() != rootPackage || !TldHelper.contains(pkgName)) // check if first level is a valid tld
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
