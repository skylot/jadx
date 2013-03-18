package jadx.dex.nodes;

import jadx.dex.attributes.AttrNode;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttribute;
import jadx.dex.attributes.annotations.Annotation;
import jadx.dex.attributes.annotations.AnnotationsList;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.FieldInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.parser.AnnotationsParser;
import jadx.dex.nodes.parser.FieldValueAttr;
import jadx.dex.nodes.parser.StaticValuesParser;
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

	private final static Logger LOG = LoggerFactory.getLogger(ClassNode.class);

	private final DexNode dex;
	private final ClassInfo clsInfo;
	private final ClassInfo superClass;
	private final List<ClassInfo> interfaces;

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

			int accFlagsValue = cls.getAccessFlags();

			IAttribute annotations = getAttributes().get(AttributeType.ANNOTATION_LIST);
			if (annotations != null) {
				AnnotationsList list = (AnnotationsList) annotations;
				Annotation iCls = list.get("dalvik.annotation.InnerClass");
				if (iCls != null)
					accFlagsValue = (Integer) iCls.getValues().get("accessFlags");
			}

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
				if (f.getType().equals(ArgType.STRING)) {
					FieldValueAttr fv = (FieldValueAttr) f.getAttributes().get(AttributeType.FIELD_VALUE);
					if (fv != null && fv.getValue() != null) {
						constFields.put(fv.getValue(), f);
					}
				}
			}
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

	public List<MethodNode> getMethods() {
		return methods;
	}

	public List<FieldNode> getFields() {
		return fields;
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

	public MethodNode searchMethodById(String shortId) {
		for (MethodNode m : methods) {
			if (m.getMethodInfo().getShortId().equals(shortId))
				return m;
		}
		return null;
	}

	public MethodNode searchMethodById(int id) {
		return searchMethodById(MethodInfo.fromDex(dex, id).getShortId());
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

	public Map<Object, FieldNode> getConstFields() {
		return constFields;
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
