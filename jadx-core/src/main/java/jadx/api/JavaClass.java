package jadx.api;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class JavaClass {

	private final Decompiler decompiler;
	private final ClassNode cls;

	private List<JavaClass> innerClasses = Collections.emptyList();
	private List<JavaField> fields = Collections.emptyList();
	private List<JavaMethod> methods = Collections.emptyList();

	JavaClass(Decompiler decompiler, ClassNode classNode) {
		this.decompiler = decompiler;
		this.cls = classNode;
	}

	public String getCode() {
		CodeWriter code = cls.getCode();
		if (code == null) {
			decompile();
			code = cls.getCode();
		}
		return code != null ? code.toString() : "error processing class";
	}

	public void decompile() {
		if (decompiler == null) {
			throw new JadxRuntimeException("Can't decompile inner class");
		}
		if (cls.getCode() == null) {
			decompiler.processClass(cls);
			load();
		}
	}

	ClassNode getClassNode() {
		return cls;
	}

	private void load() {
		int inClsCount = cls.getInnerClasses().size();
		if (inClsCount != 0) {
			List<JavaClass> list = new ArrayList<JavaClass>(inClsCount);
			for (ClassNode inner : cls.getInnerClasses()) {
				if (!inner.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
					JavaClass javaClass = new JavaClass(null, inner);
					javaClass.load();
					list.add(javaClass);
				}
			}
			this.innerClasses = Collections.unmodifiableList(list);
		}

		int fieldsCount = cls.getFields().size();
		if (fieldsCount != 0) {
			List<JavaField> flds = new ArrayList<JavaField>(fieldsCount);
			for (FieldNode f : cls.getFields()) {
				if (!f.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
					flds.add(new JavaField(f));
				}
			}
			this.fields = Collections.unmodifiableList(flds);
		}

		int methodsCount = cls.getMethods().size();
		if (methodsCount != 0) {
			List<JavaMethod> mths = new ArrayList<JavaMethod>(methodsCount);
			for (MethodNode m : cls.getMethods()) {
				if (!m.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
					mths.add(new JavaMethod(this, m));
				}
			}
			Collections.sort(mths, new Comparator<JavaMethod>() {
				@Override
				public int compare(JavaMethod o1, JavaMethod o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			this.methods = Collections.unmodifiableList(mths);
		}
	}

	private Map<CodePosition, Object> getCodeAnnotations() {
		decompile();
		return cls.getCode().getAnnotations();
	}

	public CodePosition getDefinitionPosition(int line, int offset) {
		Map<CodePosition, Object> map = getCodeAnnotations();
		Object obj = map.get(new CodePosition(line, offset));
		if (obj instanceof LineAttrNode) {
			ClassNode clsNode = null;
			if (obj instanceof ClassNode) {
				clsNode = (ClassNode) obj;
			} else if (obj instanceof MethodNode) {
				clsNode = ((MethodNode) obj).getParentClass();
			} else if (obj instanceof FieldNode) {
				clsNode = ((FieldNode) obj).getParentClass();
			}
			if (clsNode == null) {
				return null;
			}
			clsNode = clsNode.getParentClass();
			JavaClass jCls = decompiler.findJavaClass(clsNode);
			if (jCls == null) {
				return null;
			}
			jCls.decompile();
			int defLine = ((LineAttrNode) obj).getDecompiledLine();
			return new CodePosition(jCls, defLine, 0);
		}
		return null;
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

	public int getDecompiledLine() {
		return cls.getDecompiledLine();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaClass && cls.equals(((JavaClass) o).cls);
	}

	@Override
	public int hashCode() {
		return cls.hashCode();
	}

	@Override
	public String toString() {
		return getFullName();
	}
}
