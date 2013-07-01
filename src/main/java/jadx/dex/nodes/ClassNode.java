package jadx.dex.nodes;

import jadx.Consts;
import jadx.dex.attributes.AttrNode;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.SourceFileAttr;
import jadx.dex.attributes.annotations.Annotation;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.FieldInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.parser.AnnotationsParser;
import jadx.dex.nodes.parser.FieldValueAttr;
import jadx.dex.nodes.parser.StaticValuesParser;
import jadx.utils.Utils;
import jadx.utils.exceptions.DecodeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.io.ClassData;
import com.android.dx.io.ClassData.Field;
import com.android.dx.io.ClassData.Method;
import com.android.dx.io.ClassDef;

public class ClassNode extends AttrNode implements ILoadable {
	private static final Logger LOG = LoggerFactory.getLogger(ClassNode.class);

	private final DexNode dex;
	private final ClassInfo clsInfo;
	private ClassInfo superClass;
	private List<ClassInfo> interfaces;
	private Map<ArgType, List<ArgType>> genericMap;

	private final List<MethodNode> methods = new ArrayList<MethodNode>();
	private final List<FieldNode> fields = new ArrayList<FieldNode>();

	private final AccessInfo accessFlags;
	private List<ClassNode> innerClasses = Collections.emptyList();

	private final Map<Object, FieldNode> constFields = new HashMap<Object, FieldNode>();

	public ClassNode(DexNode dex, ClassDef cls) throws DecodeException {
		this.dex = dex;
		this.clsInfo = ClassInfo.fromDex(dex, cls.getTypeIndex());
		try {
			this.superClass = cls.getSupertypeIndex() == DexNode.NO_INDEX
					? null
					: ClassInfo.fromDex(dex, cls.getSupertypeIndex());

			this.interfaces = new ArrayList<ClassInfo>(cls.getInterfaces().length);
			for (short interfaceIdx : cls.getInterfaces()) {
				this.interfaces.add(ClassInfo.fromDex(dex, interfaceIdx));
			}

			if (cls.getClassDataOffset() == 0) {
				// nothing to load
			} else {
				ClassData clsData = dex.readClassData(cls);

				for (Method mth : clsData.getDirectMethods())
					methods.add(new MethodNode(this, mth));

				for (Method mth : clsData.getVirtualMethods())
					methods.add(new MethodNode(this, mth));

				for (Field f : clsData.getStaticFields())
					fields.add(new FieldNode(this, f));

				loadStaticValues(cls, fields);

				for (Field f : clsData.getInstanceFields())
					fields.add(new FieldNode(this, f));
			}

			loadAnnotations(cls);

			parseClassSignature();
			setFieldsTypesFromSignature();

			int sfIdx = cls.getSourceFileIndex();
			if (sfIdx != DexNode.NO_INDEX) {
				String fileName = dex.getString(sfIdx);
				if (!this.getFullName().contains(fileName.replace(".java", ""))) {
					this.getAttributes().add(new SourceFileAttr(fileName));
					LOG.debug("Class '{}' compiled from '{}'", this, fileName);
				}
			}

			int accFlagsValue;
			Annotation a = getAttributes().getAnnotation(Consts.DALVIK_INNER_CLASS);
			if (a != null)
				accFlagsValue = (Integer) a.getValues().get("accessFlags");
			else
				accFlagsValue = cls.getAccessFlags();

			this.accessFlags = new AccessInfo(accFlagsValue, AFType.CLASS);

		} catch (Exception e) {
			throw new DecodeException("Error decode class: " + getFullName(), e);
		}
	}

	private void loadAnnotations(ClassDef cls) {
		int offset = cls.getAnnotationsOffset();
		if (offset != 0) {
			try {
				new AnnotationsParser(this, offset);
			} catch (DecodeException e) {
				LOG.error("Error parsing annotations in " + this, e);
			}
		}
	}

	private void loadStaticValues(ClassDef cls, List<FieldNode> staticFields) throws DecodeException {
		for (FieldNode f : staticFields) {
			if (f.getAccessFlags().isFinal()) {
				FieldValueAttr nullValue = new FieldValueAttr(null);
				f.getAttributes().add(nullValue);
			}
		}

		int offset = cls.getStaticValuesOffset();
		if (offset != 0) {
			StaticValuesParser parser = new StaticValuesParser(dex, dex.openSection(offset));
			parser.processFields(staticFields);

			for (FieldNode f : staticFields) {
				AccessInfo accFlags = f.getAccessFlags();
				if (accFlags.isStatic() && accFlags.isFinal()) {
					FieldValueAttr fv = (FieldValueAttr) f.getAttributes().get(AttributeType.FIELD_VALUE);
					if (fv != null && fv.getValue() != null) {
						if (accFlags.isPublic())
							dex.getConstFields().put(fv.getValue(), f);
						else
							constFields.put(fv.getValue(), f);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parseClassSignature() {
		Annotation a = this.getAttributes().getAnnotation(Consts.DALVIK_SIGNATURE);
		if (a == null)
			return;

		String sign = Utils.mergeSignature((List<String>) a.getDefaultValue());
		// parse generic map
		int end = Utils.getGenericEnd(sign);
		if (end != -1) {
			String gen = sign.substring(1, end);
			genericMap = ArgType.parseGenericMap(gen);
			sign = sign.substring(end + 1);
		}

		// parse super class signature and interfaces
		List<ArgType> list = ArgType.parseSignatureList(sign);
		if (list != null && !list.isEmpty()) {
			try {
				ArgType st = list.remove(0);
				this.superClass = ClassInfo.fromType(st);
				int i = 0;
				for (ArgType it : list) {
					ClassInfo interf = ClassInfo.fromType(it);
					interfaces.set(i, interf);
					i++;
				}
			} catch (Throwable e) {
				LOG.warn("Can't set signatures for class: {}, sign: {}", this, sign, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setFieldsTypesFromSignature() {
		for (FieldNode field : fields) {
			Annotation a = field.getAttributes().getAnnotation(Consts.DALVIK_SIGNATURE);
			if (a == null)
				continue;

			String sign = Utils.mergeSignature((List<String>) a.getDefaultValue());
			ArgType gType = ArgType.parseSignature(sign);
			if (gType != null)
				field.setType(gType);
		}
	}

	@Override
	public void load() throws DecodeException {
		for (MethodNode mth : getMethods()) {
			mth.load();
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

	public ClassInfo getSuperClass() {
		return superClass;
	}

	public List<ClassInfo> getInterfaces() {
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

	public FieldNode getConstField(Object o) {
		FieldNode field = constFields.get(o);
		if(field == null)
			field = dex.getConstFields().get(o);
		return field;
	}

	public FieldNode searchFieldById(int id) {
		String name = FieldInfo.getNameById(dex, id);
		for (FieldNode f : fields) {
			if (f.getName().equals(name))
				return f;
		}
		return null;
	}

	public FieldNode searchField(FieldInfo field) {
		String name = field.getName();
		for (FieldNode f : fields) {
			if (f.getName().equals(name))
				return f;
		}
		return null;
	}

	public MethodNode searchMethod(MethodInfo mth) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().equals(mth))
				return m;
		}
		return null;
	}

	public MethodNode searchMethodByName(String shortId) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().getShortId().equals(shortId))
				return m;
		}
		return null;
	}

	public MethodNode searchMethodById(int id) {
		return searchMethodByName(MethodInfo.fromDex(dex, id).getShortId());
	}

	public List<ClassNode> getInnerClasses() {
		return innerClasses;
	}

	public void addInnerClass(ClassNode cls) {
		if (innerClasses.isEmpty())
			innerClasses = new ArrayList<ClassNode>(3);
		innerClasses.add(cls);
	}

	public boolean isAnonymous() {
		boolean simple = false;
		for (MethodNode m : methods) {
			MethodInfo mi = m.getMethodInfo();
			if (mi.isConstructor() && mi.getArgumentsTypes().size() == 0) {
				simple = true;
				break;
			}
		}
		return simple && Character.isDigit(getShortName().charAt(0));
	}

	public AccessInfo getAccessFlags() {
		return accessFlags;
	}

	public DexNode dex() {
		return dex;
	}

	public ClassInfo getClassInfo() {
		return clsInfo;
	}

	public String getShortName() {
		return clsInfo.getShortName();
	}

	public String getFullName() {
		return clsInfo.getFullName();
	}

	public String getPackage() {
		return clsInfo.getPackage();
	}

	@Override
	public String toString() {
		return getFullName();
	}
}
