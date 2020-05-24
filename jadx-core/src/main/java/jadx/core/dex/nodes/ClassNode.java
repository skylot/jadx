package jadx.core.dex.nodes;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import jadx.core.dex.attributes.FieldInitAttr;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.utils.SmaliUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.ProcessState.LOADED;

public class ClassNode extends NotificationAttrNode implements ILoadable, ICodeNode, Comparable<ClassNode> {
	private static final Logger LOG = LoggerFactory.getLogger(ClassNode.class);

	private final RootNode root;
	private final int clsDefOffset;
	private final Path inputPath;

	private final ClassInfo clsInfo;
	private AccessInfo accessFlags;
	private ArgType superClass;
	private List<ArgType> interfaces;
	private List<GenericTypeParameter> generics = Collections.emptyList();

	private final List<MethodNode> methods;
	private final List<FieldNode> fields;
	private List<ClassNode> innerClasses = Collections.emptyList();

	private List<ClassNode> inlinedClasses = Collections.emptyList();

	// store smali
	private String smali;
	// store parent for inner classes or 'this' otherwise
	private ClassNode parentClass;

	private volatile ProcessState state = ProcessState.NOT_LOADED;

	/** Top level classes used in this class (only for top level classes, empty for inners) */
	private List<ClassNode> dependencies = Collections.emptyList();
	/** Classes which uses this class */
	private List<ClassNode> useIn = Collections.emptyList();
	/** Methods which uses this class (by instructions only, definition is excluded) */
	private List<MethodNode> useInMth = Collections.emptyList();

	// cache maps
	private Map<MethodInfo, MethodNode> mthInfoMap = Collections.emptyMap();

	public ClassNode(RootNode root, IClassData cls) {
		this.root = root;
		this.inputPath = cls.getInputPath();
		this.clsDefOffset = cls.getClassDefOffset();
		this.clsInfo = ClassInfo.fromType(root, ArgType.object(cls.getType()));
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
			parseClassSignature();
			setFieldsTypesFromSignature();
			methods.forEach(MethodNode::initMethodTypes);

			addSourceFilenameAttr(cls.getSourceFile());
			buildCache();
		} catch (Exception e) {
			throw new JadxRuntimeException("Error decode class: " + clsInfo, e);
		}
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

	// empty synthetic class
	public ClassNode(RootNode root, String name, int accessFlags) {
		this.root = root;
		this.inputPath = null;
		this.clsDefOffset = 0;
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
				f.addAttr(FieldInitAttr.NULL_VALUE);
			}
		}
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
	}

	/**
	 * Class signature format:
	 * https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1
	 */
	private void parseClassSignature() {
		SignatureParser sp = SignatureParser.fromNode(this);
		if (sp == null) {
			return;
		}
		try {
			// parse class generic map
			generics = sp.consumeGenericTypeParameters();
			// parse super class signature
			superClass = validateSuperCls(sp.consumeType(), superClass);
			// parse interfaces signatures
			for (int i = 0; i < interfaces.size(); i++) {
				ArgType type = sp.consumeType();
				if (type != null) {
					interfaces.set(i, type);
				} else {
					break;
				}
			}
		} catch (Exception e) {
			LOG.error("Class signature parse error: {}", this, e);
		}
	}

	private ArgType validateSuperCls(ArgType candidateType, ArgType currentType) {
		if (!candidateType.isObject()) {
			this.addComment("Incorrect class signature, super class is not object: " + SignatureParser.getSignature(this));
			return currentType;
		}
		if (Objects.equals(candidateType.getObject(), this.getClassInfo().getType().getObject())) {
			this.addComment("Incorrect class signature, super class is equals to this class: " + SignatureParser.getSignature(this));
			return currentType;
		}
		return candidateType;
	}

	private void setFieldsTypesFromSignature() {
		for (FieldNode field : fields) {
			try {
				SignatureParser sp = SignatureParser.fromNode(field);
				if (sp != null) {
					ArgType gType = sp.consumeType();
					if (gType != null) {
						field.setType(gType);
					}
				}
			} catch (Exception e) {
				LOG.error("Field signature parse error: {}.{}", this.getFullName(), field.getName(), e);
			}
		}
	}

	private void addSourceFilenameAttr(String fileName) {
		if (fileName == null) {
			return;
		}
		if (fileName.endsWith(".java")) {
			fileName = fileName.substring(0, fileName.length() - 5);
		}
		if (fileName.isEmpty()
				|| fileName.equals("SourceFile")
				|| fileName.equals("\"")) {
			return;
		}
		if (clsInfo != null) {
			String name = clsInfo.getShortName();
			if (fileName.equals(name)) {
				return;
			}
			if (fileName.contains("$")
					&& fileName.endsWith('$' + name)) {
				return;
			}
			ClassInfo parentCls = clsInfo.getTopParentClass();
			if (parentCls != null && fileName.equals(parentCls.getShortName())) {
				return;
			}
		}
		this.addAttr(new SourceFileAttr(fileName));
	}

	public void ensureProcessed() {
		ClassNode topClass = getTopParentClass();
		ProcessState topState = topClass.getState();
		if (!topState.isProcessed()) {
			throw new JadxRuntimeException("Expected class to be processed at this point,"
					+ " class: " + topClass + ", state: " + topState);
		}
	}

	public ICodeInfo decompile() {
		return decompile(true);
	}

	public ICodeInfo getCode() {
		return decompile(true);
	}

	public ICodeInfo reloadCode() {
		return decompile(false);
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
		methods.forEach(MethodNode::unload);
		innerClasses.forEach(ClassNode::unload);
		fields.forEach(FieldNode::unloadAttributes);
		unloadAttributes();
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

	public List<GenericTypeParameter> getGenericTypeParameters() {
		return generics;
	}

	public List<MethodNode> getMethods() {
		return methods;
	}

	public List<FieldNode> getFields() {
		return fields;
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
			StringWriter stringWriter = new StringWriter(4096);
			getSmali(this, stringWriter);
			stringWriter.append(System.lineSeparator());
			Set<ClassNode> allInlinedClasses = new LinkedHashSet<>();
			getInnerAndInlinedClassesRecursive(allInlinedClasses);
			for (ClassNode innerClass : allInlinedClasses) {
				getSmali(innerClass, stringWriter);
				stringWriter.append(System.lineSeparator());
			}
			smali = stringWriter.toString();
		}
		return smali;
	}

	protected static boolean getSmali(ClassNode classNode, StringWriter stringWriter) {
		Path inputPath = classNode.inputPath;
		if (inputPath == null) {
			stringWriter.append(String.format("###### Class %s is created by jadx", classNode.getFullName()));
			return false;
		}
		stringWriter.append(String.format("###### Class %s (%s)", classNode.getFullName(), classNode.getRawName()));
		stringWriter.append(System.lineSeparator());
		return SmaliUtils.getSmaliCode(inputPath, classNode.clsDefOffset, stringWriter);
	}

	public ProcessState getState() {
		return state;
	}

	public void setState(ProcessState state) {
		this.state = state;
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
	public Path getInputPath() {
		return inputPath;
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
