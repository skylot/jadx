package jadx.api;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JavaClass {

	private final Decompiler decompiler;
	private final ClassNode cls;
	private final List<JavaClass> innerClasses;
	private final List<JavaField> fields;
	private final List<JavaMethod> methods;

	JavaClass(Decompiler decompiler, ClassNode classNode) {
		this.decompiler = decompiler;
		this.cls = classNode;

		int inClsCount = cls.getInnerClasses().size();
		if (inClsCount == 0) {
			this.innerClasses = Collections.emptyList();
		} else {
			List<JavaClass> list = new ArrayList<JavaClass>(inClsCount);
			for (ClassNode inner : cls.getInnerClasses()) {
				list.add(new JavaClass(decompiler, inner));
			}
			this.innerClasses = Collections.unmodifiableList(list);
		}

		int fieldsCount = cls.getFields().size();
		if (fieldsCount == 0) {
			this.fields = Collections.emptyList();
		} else {
			List<JavaField> flds = new ArrayList<JavaField>(fieldsCount);
			for (FieldNode f : cls.getFields()) {
				flds.add(new JavaField(f));
			}
			this.fields = Collections.unmodifiableList(flds);
		}

		int methodsCount = cls.getMethods().size();
		if (methodsCount == 0) {
			this.methods = Collections.emptyList();
		} else {
			List<JavaMethod> mths = new ArrayList<JavaMethod>(methodsCount);
			for (MethodNode m : cls.getMethods()) {
				if (!m.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
					mths.add(new JavaMethod(m));
				}
			}
			this.methods = Collections.unmodifiableList(mths);
		}
	}

	public String getCode() {
		CodeWriter code = cls.getCode();
		if (code == null) {
			decompiler.processClass(cls);
			code = cls.getCode();
		}
		return code != null ? code.toString() : "error processing class";
	}

	public String getFullName() {
		return cls.getFullName();
	}

	public String getShortName() {
		return cls.getShortName();
	}

	public String getPackage() {
		return cls.getPackage();
	}

	public AccessInfo getAccessInfo() {
		return cls.getAccessFlags();
	}

	public List<JavaClass> getInnerClasses() {
		return innerClasses;
	}

	public List<JavaField> getFields() {
		return fields;
	}

	public List<JavaMethod> getMethods() {
		return methods;
	}

	@Override
	public String toString() {
		return getFullName();
	}

	public int getDecompiledLine() {
		return cls.getDecompiledLine();
	}
}
