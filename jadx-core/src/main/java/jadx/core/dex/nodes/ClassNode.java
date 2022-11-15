package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.DecompilationMode;
import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.JavaClass;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultAttr;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultClassAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClassesAttr;
import jadx.api.plugins.input.data.attributes.types.InnerClsInfo;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.api.plugins.input.data.impl.ListConsumer;
import jadx.api.usage.IUsageInfoData;
import jadx.core.Consts;
import jadx.core.ProcessClass;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.InlinedAttr;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.ProcessState.LOADED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;

public class ClassNode extends NotificationAttrNode
		implements ILoadable, ICodeNode, IPackageUpdate, Comparable<ClassNode> {
	private final RootNode root;
	private final IClassData clsData;

	private final ClassInfo clsInfo;
	private PackageNode packageNode;
	private AccessInfo accessFlags;
	private ArgType superClass;
	private List<ArgType> interfaces;
	private List<ArgType> generics = Collections.emptyList();

	private List<MethodNode> methods;
	private List<FieldNode> fields;
	private List<ClassNode> innerClasses = Collections.emptyList();

	private List<ClassNode> inlinedClasses = Collections.emptyList();

	// store smali
	private String smali;
	// store parent for inner classes or 'this' otherwise
	private ClassNode parentClass;

	private volatile ProcessState state = ProcessState.NOT_LOADED;
	private LoadStage loadStage = LoadStage.NONE;

	/**
	 * Top level classes used in this class (only for top level classes, empty for inners)
	 */
	private List<ClassNode> dependencies = Collections.emptyList();
	/**
	 * Top level classes needed for code generation stage
	 */
	private List<ClassNode> codegenDeps = Collections.emptyList();
	/**
	 * Classes which uses this class
	 */
	private List<ClassNode> useIn = Collections.emptyList();
	/**
	 * Methods which uses this class (by instructions only, definition is excluded)
	 */
	private List<MethodNode> useInMth = Collections.emptyList();

	// cache maps
	private Map<MethodInfo, MethodNode> mthInfoMap = Collections.emptyMap();

	private JavaClass javaNode;

	public ClassNode(RootNode root, IClassData cls) {
		this.root = root;
		this.clsInfo = ClassInfo.fromType(root, ArgType.object(cls.getType()));
		this.packageNode = PackageNode.getForClass(root, clsInfo.getPackage(), this);
		this.clsData = cls.copy();
		load(clsData, false);
	}

	private void load(IClassData cls, boolean reloading) {
		try {
			addAttrs(cls.getAttributes());
			this.accessFlags = new AccessInfo(getAccessFlags(cls), AFType.CLASS);
			this.superClass = checkSuperType(cls);
			this.interfaces = Utils.collectionMap(cls.getInterfacesTypes(), ArgType::object);

			ListConsumer<IFieldData, FieldNode> fieldsConsumer = new ListConsumer<>(fld -> FieldNode.build(this, fld));
			ListConsumer<IMethodData, MethodNode> methodsConsumer = new ListConsumer<>(mth -> MethodNode.build(this, mth));
			cls.visitFieldsAndMethods(fieldsConsumer, methodsConsumer);
			this.fields = fieldsConsumer.getResult();
			this.methods = methodsConsumer.getResult();
			if (reloading) {
				restoreUsageData();
			}
			initStaticValues(fields);
			processAttributes(this);
			buildCache();

			// TODO: implement module attribute parsing
			if (this.accessFlags.isModuleInfo()) {
				this.addWarnComment("Modules not supported yet");
			}
		} catch (Exception e) {
			throw new JadxRuntimeException("Error decode class: " + clsInfo, e);
		}
	}

	private void restoreUsageData() {
		IUsageInfoData usageInfoData = root.getArgs().getUsageInfoCache().get(root);
		if (usageInfoData != null) {
			usageInfoData.applyForClass(this);
		}
	}

	private ArgType checkSuperType(IClassData cls) {
		String superType = cls.getSuperType();
		if (superType == null) {
			if (clsInfo.getType().getObject().equals(Consts.CLASS_OBJECT)) {
				// java.lang.Object don't have super class
				return null;
			}
			if (this.accessFlags.isModuleInfo()) {
				// module-info also don't have super class
				return null;
			}
			throw new JadxRuntimeException("No super class in " + clsInfo.getType());
		}
		return ArgType.object(superType);
	}

	public void updateGenericClsData(List<ArgType> generics, ArgType superClass, List<ArgType> interfaces) {
		this.generics = generics;
		this.superClass = superClass;
		this.interfaces = interfaces;
	}

	private static void processAttributes(ClassNode cls) {
		// move AnnotationDefault from cls to methods (dex specific)
		AnnotationDefaultClassAttr defAttr = cls.get(JadxAttrType.ANNOTATION_DEFAULT_CLASS);
		if (defAttr != null) {
			cls.remove(JadxAttrType.ANNOTATION_DEFAULT_CLASS);
			for (Map.Entry<String, EncodedValue> entry : defAttr.getValues().entrySet()) {
				MethodNode mth = cls.searchMethodByShortName(entry.getKey());
				if (mth != null) {
					mth.addAttr(new AnnotationDefaultAttr(entry.getValue()));
				} else {
					cls.addWarnComment("Method from annotation default annotation not found: " + entry.getKey());
				}
			}
		}

		// check source file attribute
		if (!cls.checkSourceFilenameAttr()) {
			cls.remove(JadxAttrType.SOURCE_FILE);
		}
	}

	private int getAccessFlags(IClassData cls) {
		InnerClassesAttr innerClassesAttr = get(JadxAttrType.INNER_CLASSES);
		if (innerClassesAttr != null) {
			InnerClsInfo innerClsInfo = innerClassesAttr.getMap().get(cls.getType());
			if (innerClsInfo != null) {
				return innerClsInfo.getAccessFlags();
			}
		}
		return cls.getAccessFlags();
	}

	public static ClassNode addSyntheticClass(RootNode root, String name, int accessFlags) {
		ClassInfo clsInfo = ClassInfo.fromName(root, name);
		ClassNode existCls = root.resolveClass(clsInfo);
		if (existCls != null) {
			throw new JadxRuntimeException("Class already exist: " + name);
		}
		return addSyntheticClass(root, clsInfo, accessFlags);
	}

	public static ClassNode addSyntheticClass(RootNode root, ClassInfo clsInfo, int accessFlags) {
		ClassNode cls = new ClassNode(root, clsInfo, accessFlags);
		cls.add(AFlag.SYNTHETIC);
		cls.setState(ProcessState.PROCESS_COMPLETE);
		root.addClassNode(cls);
		return cls;
	}

	// Create empty class
	private ClassNode(RootNode root, ClassInfo clsInfo, int accessFlags) {
		this.root = root;
		this.clsData = null;
		this.clsInfo = clsInfo;
		this.interfaces = new ArrayList<>();
		this.methods = new ArrayList<>();
		this.fields = new ArrayList<>();
		this.accessFlags = new AccessInfo(accessFlags, AFType.CLASS);
		this.parentClass = this;
		this.packageNode = PackageNode.getForClass(root, clsInfo.getPackage(), this);
	}

	private void initStaticValues(List<FieldNode> fields) {
		if (fields.isEmpty()) {
			return;
		}
		List<FieldNode> staticFields = fields.stream().filter(FieldNode::isStatic).collect(Collectors.toList());
		for (FieldNode f : staticFields) {
			if (f.getAccessFlags().isFinal() && f.get(JadxAttrType.CONSTANT_VALUE) == null) {
				// incorrect initialization will be removed if assign found in constructor
				f.addAttr(EncodedValue.NULL);
			}
		}
		try {
			// process const fields
			root().getConstValues().processConstFields(this, staticFields);
		} catch (Exception e) {
			this.addWarnComment("Failed to load initial values for static fields", e);
		}
	}

	private boolean checkSourceFilenameAttr() {
		SourceFileAttr sourceFileAttr = get(JadxAttrType.SOURCE_FILE);
		if (sourceFileAttr == null) {
			return true;
		}
		String fileName = sourceFileAttr.getFileName();
		if (fileName.endsWith(".java")) {
			fileName = fileName.substring(0, fileName.length() - 5);
		}
		if (fileName.isEmpty() || fileName.equals("SourceFile")) {
			return false;
		}
		if (clsInfo != null) {
			String name = clsInfo.getShortName();
			if (fileName.equals(name)) {
				return false;
			}
			ClassInfo parentCls = clsInfo.getParentClass();
			while (parentCls != null) {
				String parentName = parentCls.getShortName();
				if (parentName.equals(fileName) || parentName.startsWith(fileName + '$')) {
					return false;
				}
				parentCls = parentCls.getParentClass();
			}
			if (fileName.contains("$") && fileName.endsWith('$' + name)) {
				return false;
			}
			if (name.contains("$") && name.startsWith(fileName)) {
				return false;
			}
		}
		return true;
	}

	public boolean checkProcessed() {
		return getTopParentClass().getState().isProcessComplete();
	}

	public void ensureProcessed() {
		if (!checkProcessed()) {
			ClassNode topParentClass = getTopParentClass();
			throw new JadxRuntimeException("Expected class to be processed at this point,"
					+ " class: " + topParentClass + ", state: " + topParentClass.getState());
		}
	}

	public ICodeInfo decompile() {
		return decompile(true);
	}

	/**
	 * WARNING: Slow operation! Use with caution!
	 */
	public ICodeInfo decompileWithMode(DecompilationMode mode) {
		DecompilationMode baseMode = root.getArgs().getDecompilationMode();
		if (mode == baseMode) {
			return decompile(true);
		}
		JadxArgs args = root.getArgs();
		try {
			unload();
			args.setDecompilationMode(mode);
			ProcessClass process = new ProcessClass(args);
			process.initPasses(root);
			return process.generateCode(this);
		} finally {
			args.setDecompilationMode(baseMode);
		}
	}

	public ICodeInfo getCode() {
		return decompile(true);
	}

	public ICodeInfo reloadCode() {
		add(AFlag.CLASS_DEEP_RELOAD);
		return decompile(false);
	}

	public void unloadCode() {
		if (state == NOT_LOADED) {
			return;
		}
		add(AFlag.CLASS_UNLOADED);
		unloadFromCache();
		deepUnload();
	}

	public void deepUnload() {
		if (clsData == null) {
			// manually added class
			return;
		}
		unload();
		clearAttributes();
		root().getConstValues().removeForClass(this);
		load(clsData, true);

		innerClasses.forEach(ClassNode::deepUnload);
	}

	public void unloadFromCache() {
		if (isInner()) {
			return;
		}
		ICodeCache codeCache = root().getCodeCache();
		codeCache.remove(getRawName());
	}

	private synchronized ICodeInfo decompile(boolean searchInCache) {
		if (isInner()) {
			return ICodeInfo.EMPTY;
		}
		ICodeCache codeCache = root().getCodeCache();
		String clsRawName = getRawName();
		if (searchInCache) {
			ICodeInfo code = codeCache.get(clsRawName);
			if (code != ICodeInfo.EMPTY) {
				return code;
			}
		}
		ICodeInfo codeInfo;
		try {
			codeInfo = root.getProcessClasses().generateCode(this);
		} catch (Throwable e) {
			addError("Code generation failed", e);
			codeInfo = new SimpleCodeInfo(Utils.getStackTrace(e));
		}
		if (codeInfo != ICodeInfo.EMPTY) {
			codeCache.add(clsRawName, codeInfo);
		}
		return codeInfo;
	}

	@Nullable
	public ICodeInfo getCodeFromCache() {
		ICodeCache codeCache = root().getCodeCache();
		String clsRawName = getRawName();
		ICodeInfo codeInfo = codeCache.get(clsRawName);
		if (codeInfo == ICodeInfo.EMPTY) {
			return null;
		}
		return codeInfo;
	}

	@Override
	public void load() {
		for (MethodNode mth : getMethods()) {
			try {
				mth.load();
			} catch (Exception e) {
				mth.addError("Method load error", e);
			}
		}
		for (ClassNode innerCls : getInnerClasses()) {
			innerCls.load();
		}
		setState(LOADED);
	}

	@Override
	public void unload() {
		if (state == NOT_LOADED) {
			return;
		}
		methods.forEach(MethodNode::unload);
		innerClasses.forEach(ClassNode::unload);
		fields.forEach(FieldNode::unloadAttributes);
		unloadAttributes();
		setState(NOT_LOADED);
		this.loadStage = LoadStage.NONE;
		this.smali = null;
	}

	private void buildCache() {
		mthInfoMap = new HashMap<>(methods.size());
		for (MethodNode mth : methods) {
			mthInfoMap.put(mth.getMethodInfo(), mth);
		}
	}

	@Nullable
	public ArgType getSuperClass() {
		return superClass;
	}

	public List<ArgType> getInterfaces() {
		return interfaces;
	}

	public List<ArgType> getGenericTypeParameters() {
		return generics;
	}

	public ArgType getType() {
		ArgType clsType = clsInfo.getType();
		if (Utils.notEmpty(generics)) {
			return ArgType.generic(clsType, generics);
		}
		return clsType;
	}

	public List<MethodNode> getMethods() {
		return methods;
	}

	public List<FieldNode> getFields() {
		return fields;
	}

	public void addField(FieldNode fld) {
		if (fields == null || fields.isEmpty()) {
			fields = new ArrayList<>(1);
		}
		fields.add(fld);
	}

	public FieldNode getConstField(Object obj) {
		return getConstField(obj, true);
	}

	@Nullable
	public FieldNode getConstField(Object obj, boolean searchGlobal) {
		return root().getConstValues().getConstField(this, obj, searchGlobal);
	}

	@Nullable
	public FieldNode getConstFieldByLiteralArg(LiteralArg arg) {
		return root().getConstValues().getConstFieldByLiteralArg(this, arg);
	}

	public FieldNode searchField(FieldInfo field) {
		for (FieldNode f : fields) {
			if (f.getFieldInfo().equals(field)) {
				return f;
			}
		}
		return null;
	}

	public FieldNode searchFieldByNameAndType(FieldInfo field) {
		for (FieldNode f : fields) {
			if (f.getFieldInfo().equalsNameAndType(field)) {
				return f;
			}
		}
		return null;
	}

	public FieldNode searchFieldByName(String name) {
		for (FieldNode f : fields) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	public FieldNode searchFieldByShortId(String shortId) {
		for (FieldNode f : fields) {
			if (f.getFieldInfo().getShortId().equals(shortId)) {
				return f;
			}
		}
		return null;
	}

	public MethodNode searchMethod(MethodInfo mth) {
		return mthInfoMap.get(mth);
	}

	public MethodNode searchMethodByShortId(String shortId) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().getShortId().equals(shortId)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * Return first method by original short name
	 * Note: methods are not unique by name (class can have several methods with same name but different
	 * signature)
	 */
	@Nullable
	public MethodNode searchMethodByShortName(String name) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public ClassNode getParentClass() {
		return parentClass;
	}

	public void updateParentClass() {
		if (clsInfo.isInner()) {
			ClassNode parent = root.resolveClass(clsInfo.getParentClass());
			if (parent != null) {
				parentClass = parent;
				return;
			}
		}
		parentClass = this;
	}

	/**
	 * Change class name and package (if full name provided)
	 * Leading dot can be used to move to default package.
	 * Package for inner classes can't be changed.
	 */
	@Override
	public void rename(String newName) {
		int lastDot = newName.lastIndexOf('.');
		if (lastDot == -1) {
			clsInfo.changeShortName(newName);
			return;
		}
		if (isInner()) {
			addWarn("Can't change package for inner class: " + this + " to " + newName);
			return;
		}
		// change class package
		String newPkg = newName.substring(0, lastDot);
		String newShortName = newName.substring(lastDot + 1);
		if (changeClassNodePackage(newPkg)) {
			clsInfo.changePkgAndName(newPkg, newShortName);
		} else {
			clsInfo.changeShortName(newShortName);
		}
	}

	private boolean changeClassNodePackage(String fullPkg) {
		if (clsInfo.isInner()) {
			throw new JadxRuntimeException("Can't change package for inner class");
		}
		if (fullPkg.equals(clsInfo.getAliasPkg())) {
			return false;
		}
		root.removeClsFromPackage(packageNode, this);
		packageNode = PackageNode.getForClass(root, fullPkg, this);
		root.sortPackages();
		return true;
	}

	public void removeAlias() {
		if (!clsInfo.isInner()) {
			changeClassNodePackage(clsInfo.getPackage());
		}
		clsInfo.removeAlias();
	}

	@Override
	public void onParentPackageUpdate(PackageNode updatedPkg) {
		if (isInner()) {
			return;
		}
		clsInfo.changePkg(packageNode.getAliasPkgInfo().getFullName());
	}

	public PackageNode getPackageNode() {
		return packageNode;
	}

	public ClassNode getTopParentClass() {
		ClassNode parent = getParentClass();
		return parent == this ? this : parent.getTopParentClass();
	}

	public void visitParentClasses(Consumer<ClassNode> consumer) {
		ClassNode currentCls = this;
		ClassNode parentCls = currentCls.getParentClass();
		while (parentCls != currentCls) {
			consumer.accept(parentCls);
			currentCls = parentCls;
			parentCls = currentCls.getParentClass();
		}
	}

	public void visitSuperTypes(BiConsumer<ArgType, ArgType> consumer) {
		TypeUtils typeUtils = root.getTypeUtils();
		ArgType thisType = this.getType();
		if (!superClass.equals(ArgType.OBJECT)) {
			consumer.accept(thisType, superClass);
			typeUtils.visitSuperTypes(superClass, consumer);
		}
		for (ArgType iface : interfaces) {
			consumer.accept(thisType, iface);
			typeUtils.visitSuperTypes(iface, consumer);
		}
	}

	public boolean hasNotGeneratedParent() {
		if (contains(AFlag.DONT_GENERATE)) {
			return true;
		}
		ClassNode parent = getParentClass();
		if (parent == this) {
			return false;
		}
		return parent.hasNotGeneratedParent();
	}

	public List<ClassNode> getInnerClasses() {
		return innerClasses;
	}

	public List<ClassNode> getInlinedClasses() {
		return inlinedClasses;
	}

	/**
	 * Get all inner and inlined classes recursively
	 *
	 * @param resultClassesSet
	 *                         all identified inner and inlined classes are added to this set
	 */
	public void getInnerAndInlinedClassesRecursive(Set<ClassNode> resultClassesSet) {
		for (ClassNode innerCls : innerClasses) {
			if (resultClassesSet.add(innerCls)) {
				innerCls.getInnerAndInlinedClassesRecursive(resultClassesSet);
			}
		}
		for (ClassNode inlinedCls : inlinedClasses) {
			if (resultClassesSet.add(inlinedCls)) {
				inlinedCls.getInnerAndInlinedClassesRecursive(resultClassesSet);
			}
		}
	}

	public void addInnerClass(ClassNode cls) {
		if (innerClasses.isEmpty()) {
			innerClasses = new ArrayList<>(5);
		}
		innerClasses.add(cls);
		cls.parentClass = this;
	}

	public void addInlinedClass(ClassNode cls) {
		if (inlinedClasses.isEmpty()) {
			inlinedClasses = new ArrayList<>(5);
		}
		cls.addAttr(new InlinedAttr(this));
		inlinedClasses.add(cls);
	}

	public boolean isEnum() {
		return getAccessFlags().isEnum()
				&& getSuperClass() != null
				&& getSuperClass().getObject().equals(ArgType.ENUM.getObject());
	}

	public boolean isAnonymous() {
		return contains(AType.ANONYMOUS_CLASS);
	}

	public boolean isSynthetic() {
		return contains(AFlag.SYNTHETIC);
	}

	public boolean isInner() {
		return parentClass != this;
	}

	public boolean isTopClass() {
		return parentClass == this;
	}

	@Nullable
	public MethodNode getClassInitMth() {
		return searchMethodByShortId("<clinit>()V");
	}

	@Nullable
	public MethodNode getDefaultConstructor() {
		for (MethodNode mth : methods) {
			if (mth.isDefaultConstructor()) {
				return mth;
			}
		}
		return null;
	}

	@Override
	public AccessInfo getAccessFlags() {
		return accessFlags;
	}

	@Override
	public void setAccessFlags(AccessInfo accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Override
	public RootNode root() {
		return root;
	}

	@Override
	public String typeName() {
		return "class";
	}

	public String getRawName() {
		return clsInfo.getRawName();
	}

	/**
	 * Internal class info (don't use in code generation and external api).
	 */
	public ClassInfo getClassInfo() {
		return clsInfo;
	}

	public String getName() {
		return clsInfo.getShortName();
	}

	public String getAlias() {
		return clsInfo.getAliasShortName();
	}

	@Deprecated
	public String getShortName() {
		return clsInfo.getAliasShortName();
	}

	public String getFullName() {
		return clsInfo.getAliasFullName();
	}

	public String getPackage() {
		return clsInfo.getAliasPkg();
	}

	public String getDisassembledCode() {
		if (smali == null) {
			StringBuilder sb = new StringBuilder();
			getDisassembledCode(sb);
			sb.append(ICodeWriter.NL);
			Set<ClassNode> allInlinedClasses = new LinkedHashSet<>();
			getInnerAndInlinedClassesRecursive(allInlinedClasses);
			for (ClassNode innerClass : allInlinedClasses) {
				innerClass.getDisassembledCode(sb);
				sb.append(ICodeWriter.NL);
			}
			smali = sb.toString();
		}
		return smali;
	}

	protected void getDisassembledCode(StringBuilder sb) {
		if (clsData == null) {
			sb.append(String.format("###### Class %s is created by jadx", getFullName()));
			return;
		}
		sb.append(String.format("###### Class %s (%s)", getFullName(), getRawName()));
		sb.append(ICodeWriter.NL);
		sb.append(clsData.getDisassembledCode());
	}

	public IClassData getClsData() {
		return clsData;
	}

	public ProcessState getState() {
		return state;
	}

	public void setState(ProcessState state) {
		this.state = state;
	}

	public LoadStage getLoadStage() {
		return loadStage;
	}

	public void setLoadStage(LoadStage loadStage) {
		this.loadStage = loadStage;
	}

	public void reloadAtCodegenStage() {
		ClassNode topCls = this.getTopParentClass();
		if (topCls.getLoadStage() == LoadStage.CODEGEN_STAGE) {
			throw new JadxRuntimeException("Class not yet loaded at codegen stage: " + topCls);
		}
		topCls.add(AFlag.RELOAD_AT_CODEGEN_STAGE);
	}

	public List<ClassNode> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<ClassNode> dependencies) {
		this.dependencies = dependencies;
	}

	public void removeDependency(ClassNode dep) {
		this.dependencies = ListUtils.safeRemoveAndTrim(this.dependencies, dep);
	}

	public List<ClassNode> getCodegenDeps() {
		return codegenDeps;
	}

	public void setCodegenDeps(List<ClassNode> codegenDeps) {
		this.codegenDeps = codegenDeps;
	}

	public void addCodegenDep(ClassNode dep) {
		if (!codegenDeps.contains(dep)) {
			this.codegenDeps = ListUtils.safeAdd(this.codegenDeps, dep);
		}
	}

	public int getTotalDepsCount() {
		return dependencies.size() + codegenDeps.size();
	}

	public List<ClassNode> getUseIn() {
		return useIn;
	}

	public void setUseIn(List<ClassNode> useIn) {
		this.useIn = useIn;
	}

	public List<MethodNode> getUseInMth() {
		return useInMth;
	}

	public void setUseInMth(List<MethodNode> useInMth) {
		this.useInMth = useInMth;
	}

	@Override
	public String getInputFileName() {
		return clsData == null ? "synthetic" : clsData.getInputFileName();
	}

	public JavaClass getJavaNode() {
		return javaNode;
	}

	public void setJavaNode(JavaClass javaNode) {
		this.javaNode = javaNode;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.CLASS;
	}

	@Override
	public int hashCode() {
		return clsInfo.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof ClassNode) {
			ClassNode other = (ClassNode) o;
			return clsInfo.equals(other.clsInfo);
		}
		return false;
	}

	@Override
	public int compareTo(@NotNull ClassNode o) {
		return this.clsInfo.compareTo(o.clsInfo);
	}

	@Override
	public String toString() {
		return clsInfo.getFullName();
	}
}
