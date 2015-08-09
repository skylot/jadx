package jadx.core.dex.nodes;

import jadx.core.Consts;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.annotations.Annotation;
import jadx.core.dex.attributes.nodes.JadxErrorAttr;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.parser.AnnotationsParser;
import jadx.core.dex.nodes.parser.FieldInitAttr;
import jadx.core.dex.nodes.parser.FieldInitAttr.InitType;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.dex.nodes.parser.StaticValuesParser;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Field;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dx.rop.code.AccessFlags;

public class ClassNode extends LineAttrNode implements ILoadable {
	private static final Logger LOG = LoggerFactory.getLogger(ClassNode.class);

	private final DexNode dex;
	private final ClassInfo clsInfo;
	private final AccessInfo accessFlags;
	private ArgType superClass;
	private List<ArgType> interfaces;
	private Map<ArgType, List<ArgType>> genericMap;

	private final List<MethodNode> methods;
	private final List<FieldNode> fields;
	private Map<Object, FieldNode> constFields = Collections.emptyMap();
	private List<ClassNode> innerClasses = Collections.emptyList();

	// store decompiled code
	private CodeWriter code;
	// store parent for inner classes or 'this' otherwise
	private ClassNode parentClass;

	private ProcessState state = ProcessState.NOT_LOADED;
	private final Set<ClassNode> dependencies = new HashSet<ClassNode>();

	// cache maps
	private Map<MethodInfo, MethodNode> mthInfoMap = Collections.emptyMap();

	public ClassNode(DexNode dex, ClassDef cls) throws DecodeException {
		this.dex = dex;
		this.clsInfo = ClassInfo.fromDex(dex, cls.getTypeIndex());
		try {
			if (cls.getSupertypeIndex() == DexNode.NO_INDEX) {
				this.superClass = null;
			} else {
				this.superClass = dex.getType(cls.getSupertypeIndex());
			}
			this.interfaces = new ArrayList<ArgType>(cls.getInterfaces().length);
			for (short interfaceIdx : cls.getInterfaces()) {
				this.interfaces.add(dex.getType(interfaceIdx));
			}
			if (cls.getClassDataOffset() != 0) {
				ClassData clsData = dex.readClassData(cls);
				int mthsCount = clsData.getDirectMethods().length + clsData.getVirtualMethods().length;
				int fieldsCount = clsData.getStaticFields().length + clsData.getInstanceFields().length;

				methods = new ArrayList<MethodNode>(mthsCount);
				fields = new ArrayList<FieldNode>(fieldsCount);

				for (Method mth : clsData.getDirectMethods()) {
					methods.add(new MethodNode(this, mth, false));
				}
				for (Method mth : clsData.getVirtualMethods()) {
					methods.add(new MethodNode(this, mth, true));
				}

				for (Field f : clsData.getStaticFields()) {
					fields.add(new FieldNode(this, f));
				}
				loadStaticValues(cls, fields);
				for (Field f : clsData.getInstanceFields()) {
					fields.add(new FieldNode(this, f));
				}
			} else {
				methods = Collections.emptyList();
				fields = Collections.emptyList();
			}

			loadAnnotations(cls);

			parseClassSignature();
			setFieldsTypesFromSignature();

			int sfIdx = cls.getSourceFileIndex();
			if (sfIdx != DexNode.NO_INDEX) {
				String fileName = dex.getString(sfIdx);
				addSourceFilenameAttr(fileName);
			}

			// restore original access flags from dalvik annotation if present
			int accFlagsValue;
			Annotation a = getAnnotation(Consts.DALVIK_INNER_CLASS);
			if (a != null) {
				accFlagsValue = (Integer) a.getValues().get("accessFlags");
			} else {
				accFlagsValue = cls.getAccessFlags();
			}
			this.accessFlags = new AccessInfo(accFlagsValue, AFType.CLASS);

			buildCache();
		} catch (Exception e) {
			throw new DecodeException("Error decode class: " + clsInfo, e);
		}
	}

	// empty synthetic class
	public ClassNode(DexNode dex, ClassInfo clsInfo) {
		this.dex = dex;
		this.clsInfo = clsInfo;
		this.interfaces = Collections.emptyList();
		this.methods = Collections.emptyList();
		this.fields = Collections.emptyList();
		this.accessFlags = new AccessInfo(AccessFlags.ACC_PUBLIC | AccessFlags.ACC_SYNTHETIC, AFType.CLASS);
		this.parentClass = this;
	}

	private void loadAnnotations(ClassDef cls) {
		int offset = cls.getAnnotationsOffset();
		if (offset != 0) {
			try {
				new AnnotationsParser(this).parse(offset);
			} catch (Exception e) {
				LOG.error("Error parsing annotations in {}", this, e);
			}
		}
	}

	private void loadStaticValues(ClassDef cls, List<FieldNode> staticFields) throws DecodeException {
		for (FieldNode f : staticFields) {
			if (f.getAccessFlags().isFinal()) {
				f.addAttr(FieldInitAttr.NULL_VALUE);
			}
		}
		int offset = cls.getStaticValuesOffset();
		if (offset == 0) {
			return;
		}
		StaticValuesParser parser = new StaticValuesParser(dex, dex.openSection(offset));
		int count = parser.processFields(staticFields);
		if (count == 0) {
			return;
		}
		constFields = new LinkedHashMap<Object, FieldNode>(count);
		for (FieldNode f : staticFields) {
			AccessInfo accFlags = f.getAccessFlags();
			if (accFlags.isStatic() && accFlags.isFinal()) {
				FieldInitAttr fv = f.get(AType.FIELD_INIT);
				if (fv != null && fv.getValue() != null && fv.getValueType() == InitType.CONST) {
					if (accFlags.isPublic()) {
						dex.getConstFields().put(fv.getValue(), f);
					}
					constFields.put(fv.getValue(), f);
				}
			}
		}
	}

	private void parseClassSignature() {
		SignatureParser sp = SignatureParser.fromNode(this);
		if (sp == null) {
			return;
		}
		try {
			// parse class generic map
			genericMap = sp.consumeGenericMap();
			// parse super class signature
			superClass = sp.consumeType();
			// parse interfaces signatures
			for (int i = 0; i < interfaces.size(); i++) {
				ArgType type = sp.consumeType();
				if (type != null) {
					interfaces.set(i, type);
				} else {
					break;
				}
			}
		} catch (JadxRuntimeException e) {
			LOG.error("Class signature parse error: {}", this, e);
		}
	}

	private void setFieldsTypesFromSignature() {
		for (FieldNode field : fields) {
			SignatureParser sp = SignatureParser.fromNode(field);
			if (sp != null) {
				try {
					ArgType gType = sp.consumeType();
					if (gType != null) {
						field.setType(gType);
					}
				} catch (JadxRuntimeException e) {
					LOG.error("Field signature parse error: {}", field, e);
				}
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
					&& fileName.endsWith("$" + name)) {
				return;
			}
			ClassInfo parentClass = clsInfo.getTopParentClass();
			if (parentClass != null && fileName.equals(parentClass.getShortName())) {
				return;
			}
		}
		this.addAttr(new SourceFileAttr(fileName));
		LOG.debug("Class '{}' compiled from '{}'", this, fileName);
	}

	@Override
	public void load() {
		for (MethodNode mth : getMethods()) {
			try {
				mth.load();
			} catch (Exception e) {
				LOG.error("Method load error: {}", mth, e);
				mth.addAttr(new JadxErrorAttr(e));
			}
		}
		for (ClassNode innerCls : getInnerClasses()) {
			innerCls.load();
		}
	}

	@Override
	public void unload() {
		for (MethodNode mth : getMethods()) {
			mth.unload();
		}
		for (ClassNode innerCls : getInnerClasses()) {
			innerCls.unload();
		}
	}

	private void buildCache() {
		mthInfoMap = new HashMap<MethodInfo, MethodNode>(methods.size());
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

	public Map<ArgType, List<ArgType>> getGenericMap() {
		return genericMap;
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

	public FieldNode getConstField(Object obj, boolean searchGlobal) {
		ClassNode cn = this;
		FieldNode field;
		do {
			field = cn.constFields.get(obj);
		}
		while (field == null
				&& cn.clsInfo.getParentClass() != null
				&& (cn = dex.resolveClass(cn.clsInfo.getParentClass())) != null);

		if (field == null && searchGlobal) {
			field = dex.getConstFields().get(obj);
		}
		if (obj instanceof Integer) {
			String str = dex.root().getResourcesNames().get(obj);
			if (str != null) {
				ResRefField resField = new ResRefField(dex, str.replace('/', '.'));
				if (field == null) {
					return resField;
				}
				if (!field.getName().equals(resField.getName())) {
					field = resField;
				}
			}
		}
		return field;
	}

	public FieldNode getConstFieldByLiteralArg(LiteralArg arg) {
		PrimitiveType type = arg.getType().getPrimitiveType();
		if (type == null) {
			return null;
		}
		long literal = arg.getLiteral();
		switch (type) {
			case BOOLEAN:
				return getConstField(literal == 1, false);
			case CHAR:
				return getConstField((char) literal, Math.abs(literal) > 10);
			case BYTE:
				return getConstField((byte) literal, Math.abs(literal) > 10);
			case SHORT:
				return getConstField((short) literal, Math.abs(literal) > 100);
			case INT:
				return getConstField((int) literal, Math.abs(literal) > 100);
			case LONG:
				return getConstField(literal, Math.abs(literal) > 1000);
			case FLOAT:
				float f = Float.intBitsToFloat((int) literal);
				return getConstField(f, f != 0.0);
			case DOUBLE:
				double d = Double.longBitsToDouble(literal);
				return getConstField(d, d != 0);
		}
		return null;
	}

	public FieldNode searchFieldById(int id) {
		return searchField(FieldInfo.fromDex(dex, id));
	}

	public FieldNode searchField(FieldInfo field) {
		for (FieldNode f : fields) {
			if (f.getFieldInfo().equals(field)) {
				return f;
			}
		}
		return null;
	}

	@TestOnly
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

	public MethodNode searchMethodByName(String shortId) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().getShortId().equals(shortId)) {
				return m;
			}
		}
		return null;
	}

	public MethodNode searchMethodById(int id) {
		return searchMethodByName(MethodInfo.fromDex(dex, id).getShortId());
	}

	public ClassNode getParentClass() {
		if (parentClass == null) {
			if (clsInfo.isInner()) {
				ClassNode parent = dex().resolveClass(clsInfo.getParentClass());
				parent = parent == null ? this : parent;
				parentClass = parent;
			} else {
				parentClass = this;
			}
		}
		return parentClass;
	}

	public ClassNode getTopParentClass() {
		ClassNode parent = getParentClass();
		return parent == this ? this : parent.getParentClass();
	}

	public List<ClassNode> getInnerClasses() {
		return innerClasses;
	}

	public void addInnerClass(ClassNode cls) {
		if (innerClasses.isEmpty()) {
			innerClasses = new ArrayList<ClassNode>(3);
		}
		innerClasses.add(cls);
	}

	public boolean isEnum() {
		return getAccessFlags().isEnum()
				&& getSuperClass() != null
				&& getSuperClass().getObject().equals(ArgType.ENUM.getObject());
	}

	public boolean isAnonymous() {
		return clsInfo.isInner()
				&& clsInfo.getAlias().getShortName().startsWith(Consts.ANONYMOUS_CLASS_PREFIX)
				&& getDefaultConstructor() != null;
	}

	@Nullable
	public MethodNode getClassInitMth() {
		return searchMethodByName("<clinit>()V");
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

	public AccessInfo getAccessFlags() {
		return accessFlags;
	}

	public DexNode dex() {
		return dex;
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

	/**
	 * Class info for external usage (code generation and external api).
	 */
	public ClassInfo getAlias() {
		return clsInfo.getAlias();
	}

	public String getShortName() {
		return clsInfo.getAlias().getShortName();
	}

	public String getFullName() {
		return clsInfo.getAlias().getFullName();
	}

	public String getPackage() {
		return clsInfo.getAlias().getPackage();
	}

	public void setCode(CodeWriter code) {
		this.code = code;
	}

	public CodeWriter getCode() {
		return code;
	}

	public ProcessState getState() {
		return state;
	}

	public void setState(ProcessState state) {
		this.state = state;
	}

	public Set<ClassNode> getDependencies() {
		return dependencies;
	}

	@Override
	public String toString() {
		return clsInfo.getFullName();
	}
}
