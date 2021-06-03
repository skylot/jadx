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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.Consts;
import jadx.core.ProcessClass;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.fldinit.FieldInitAttr;
import jadx.core.dex.attributes.fldinit.FieldInitConstAttr;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.ProcessState.LOADED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESS_COMPLETE;

public class ClassNode extends NotificationAttrNode implements ILoadable, ICodeNode, Comparable<ClassNode> {
	private static final Logger LOG = LoggerFactory.getLogger(ClassNode.class);

	private final RootNode root;
	private final IClassData clsData;

	private final ClassInfo clsInfo;
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
	 * Classes which uses this class
	 */
	private List<ClassNode> useIn = Collections.emptyList();
	/**
	 * Methods which uses this class (by instructions only, definition is excluded)
	 */
	private List<MethodNode> useInMth = Collections.emptyList();

	// cache maps
	private Map<MethodInfo, MethodNode> mthInfoMap = Collections.emptyMap();

	public ClassNode(RootNode root, IClassData cls) {
		this.root = root;
		this.clsInfo = ClassInfo.fromType(root, ArgType.object(cls.getType()));
		this.clsData = cls.copy();
		initialLoad(clsData);
	}

	private void initialLoad(IClassData cls) {
		try {
			String superType = cls.getSuperType();
			if (superType == null) {
				// only java.lang.Object don't have super class
				if (!clsInfo.getType().getObject().equals(Consts.CLASS_OBJECT)) {
					throw new JadxRuntimeException("No super class in " + clsInfo.getType());
				}
				this.superClass = null;
			} else {
				this.superClass = ArgType.object(superType);
			}
			this.interfaces = Utils.collectionMap(cls.getInterfacesTypes(), ArgType::object);

			methods = new ArrayList<>();
			fields = new ArrayList<>();
			cls.visitFieldsAndMethods(
					fld -> fields.add(FieldNode.build(this, fld)),
					mth -> methods.add(MethodNode.build(this, mth)));

			AnnotationsList.attach(this, cls.getAnnotations());
			loadStaticValues(cls, fields);
			initAccessFlags(cls);

			addSourceFilenameAttr(cls.getSourceFile());
			buildCache();
		} catch (Exception e) {
			throw new JadxRuntimeException("Error decode class: " + clsInfo, e);
		}
	}

	public void updateGenericClsData(ArgType superClass, List<ArgType> interfaces, List<ArgType> generics) {
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.generics = generics;
	}

	/**
	 * Restore original access flags from Dalvik annotation if present
	 */
	private void initAccessFlags(IClassData cls) {
		int accFlagsValue;
		IAnnotation a = getAnnotation(Consts.DALVIK_INNER_CLASS);
		if (a != null) {
			accFlagsValue = (Integer) a.getValues().get("accessFlags").getValue();
		} else {
			accFlagsValue = cls.getAccessFlags();
		}
		this.accessFlags = new AccessInfo(accFlagsValue, AFType.CLASS);
	}

	public static ClassNode addSyntheticClass(RootNode root, String name, int accessFlags) {
		ClassNode cls = new ClassNode(root, name, accessFlags);
		cls.add(AFlag.SYNTHETIC);
		cls.setState(ProcessState.PROCESS_COMPLETE);
		root.addClassNode(cls);
		return cls;
	}

	// Create empty class
	private ClassNode(RootNode root, String name, int accessFlags) {
		this.root = root;
		this.clsData = null;
		this.clsInfo = ClassInfo.fromName(root, name);
		this.interfaces = new ArrayList<>();
		this.methods = new ArrayList<>();
		this.fields = new ArrayList<>();
		this.accessFlags = new AccessInfo(accessFlags, AFType.CLASS);
		this.parentClass = this;
	}

	private void loadStaticValues(IClassData cls, List<FieldNode> fields) {
		if (fields.isEmpty()) {
			return;
		}
		List<FieldNode> staticFields = fields.stream().filter(FieldNode::isStatic).collect(Collectors.toList());
		for (FieldNode f : staticFields) {
			if (f.getAccessFlags().isFinal()) {
				// incorrect initialization will be removed if assign found in constructor
				f.addAttr(FieldInitConstAttr.NULL_VALUE);
			}
		}
		try {
			List<EncodedValue> values = cls.getStaticFieldInitValues();
			int count = values.size();
			if (count == 0 || count > staticFields.size()) {
				return;
			}
			for (int i = 0; i < count; i++) {
				staticFields.get(i).addAttr(FieldInitAttr.constValue(values.get(i)));
			}
			// process const fields
			root().getConstValues().processConstFields(this, staticFields);
		} catch (Exception e) {
			this.addWarnComment("Failed to load initial values for static fields", e);
		}
	}

	private void addSourceFilenameAttr(String fileName) {
		if (fileName == null) {
			return;
		}
		if (fileName.endsWith(".java")) {
			fileName = fileName.substring(0, fileName.length() - 5);
		}
		if (fileName.isEmpty() || fileName.equals("SourceFile")) {
			return;
		}
		if (clsInfo != null) {
			String name = clsInfo.getShortName();
			if (fileName.equals(name)) {
				return;
			}
			if (fileName.contains("$") && fileName.endsWith('$' + name)) {
				return;
			}
		}
		this.addAttr(new SourceFileAttr(fileName));
	}

	public void ensureProcessed() {
		ClassNode topClass = getTopParentClass();
		ProcessState state = topClass.getState();
		if (state != PROCESS_COMPLETE) {
			throw new JadxRuntimeException("Expected class to be processed at this point,"
					+ " class: " + topClass + ", state: " + state);
		}
	}

	public ICodeInfo decompile() {
		return decompile(true);
	}

	public ICodeInfo getCode() {
		return decompile(true);
	}

	public ICodeInfo reloadCode() {
		add(AFlag.CLASS_DEEP_RELOAD);
		return decompile(false);
	}

	public void deepUnload() {
		if (clsData == null) {
			// manually added class
			return;
		}
		unload();
		clearAttributes();
		root().getConstValues().removeForClass(this);
		initialLoad(clsData);

		innerClasses.forEach(ClassNode::deepUnload);
	}

	private synchronized ICodeInfo decompile(boolean searchInCache) {
		ICodeCache codeCache = root().getCodeCache();
		ClassNode topParentClass = getTopParentClass();
		String clsRawName = topParentClass.getRawName();
		if (searchInCache) {
			ICodeInfo code = codeCache.get(clsRawName);
			if (code != null && code != ICodeInfo.EMPTY) {
				return code;
			}
		}
		ICodeInfo codeInfo = ProcessClass.generateCode(topParentClass);
		codeCache.add(clsRawName, codeInfo);
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
		if (parentClass == null) {
			if (clsInfo.isInner()) {
				ClassNode parent = root.resolveClass(clsInfo.getParentClass());
				parentClass = parent == null ? this : parent;
			} else {
				parentClass = this;
			}
		}
		return parentClass;
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

	/**
	 * Get all inner and inlined classes recursively
	 *
	 * @param resultClassesSet all identified inner and inlined classes are added to this set
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
		inlinedClasses.add(cls);
	}

	public boolean isEnum() {
		return getAccessFlags().isEnum()
				&& getSuperClass() != null
				&& getSuperClass().getObject().equals(ArgType.ENUM.getObject());
	}

	public boolean isAnonymous() {
		return contains(AFlag.ANONYMOUS_CLASS);
	}

	public boolean isInner() {
		return parentClass != null;
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

	public String getShortName() {
		return clsInfo.getAliasShortName();
	}

	public String getFullName() {
		return clsInfo.getAliasFullName();
	}

	public String getPackage() {
		return clsInfo.getAliasPkg();
	}

	public String getSmali() {
		if (smali == null) {
			StringBuilder sb = new StringBuilder();
			getSmali(sb);
			sb.append(System.lineSeparator());
			Set<ClassNode> allInlinedClasses = new LinkedHashSet<>();
			getInnerAndInlinedClassesRecursive(allInlinedClasses);
			for (ClassNode innerClass : allInlinedClasses) {
				innerClass.getSmali(sb);
				sb.append(System.lineSeparator());
			}
			smali = sb.toString();
		}
		return smali;
	}

	protected void getSmali(StringBuilder sb) {
		if (this.clsData == null) {
			sb.append(String.format("###### Class %s is created by jadx", getFullName()));
			return;
		}
		sb.append(String.format("###### Class %s (%s)", getFullName(), getRawName()));
		sb.append(System.lineSeparator());
		sb.append(this.clsData.getDisassembledCode());
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
		return this.getFullName().compareTo(o.getFullName());
	}

	@Override
	public String toString() {
		return clsInfo.getFullName();
	}
}
