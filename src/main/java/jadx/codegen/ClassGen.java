package jadx.codegen;

import jadx.Consts;
import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.EnumClassAttr;
import jadx.dex.attributes.EnumClassAttr.EnumField;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.ClassInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.FieldNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.nodes.parser.FieldValueAttr;
import jadx.utils.ErrorsCounter;
import jadx.utils.Utils;
import jadx.utils.exceptions.CodegenException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.dx.rop.code.AccessFlags;

public class ClassGen {
	private final ClassNode cls;
	private final ClassGen parentGen;
	private final AnnotationGen annotationGen;
	private final boolean fallback;

	private final Set<ClassInfo> imports = new HashSet<ClassInfo>();

	public ClassGen(ClassNode cls, ClassGen parentClsGen, boolean fallback) {
		this.cls = cls;
		this.parentGen = parentClsGen;
		this.fallback = fallback;

		this.annotationGen = new AnnotationGen(cls, this);
	}

	public ClassNode getClassNode() {
		return cls;
	}

	public CodeWriter makeClass() throws CodegenException {
		CodeWriter clsBody = new CodeWriter();
		addClassCode(clsBody);

		CodeWriter clsCode = new CodeWriter();

		if (!"".equals(cls.getPackage())) {
			clsCode.add("package ").add(cls.getPackage()).add(";");
			clsCode.endl();
		}

		if (imports.size() != 0) {
			List<String> sortImports = new ArrayList<String>();
			for (ClassInfo ic : imports)
				sortImports.add(ic.getFullName());
			Collections.sort(sortImports);

			for (String imp : sortImports) {
				clsCode.startLine("import ").add(imp).add(";");
			}
			clsCode.endl();

			sortImports.clear();
			imports.clear();
		}

		clsCode.add(clsBody);
		return clsCode;
	}

	public void addClassCode(CodeWriter code) throws CodegenException {
		if (cls.getAttributes().contains(AttributeFlag.DONT_GENERATE))
			return;

		makeClassDeclaration(code);
		makeClassBody(code);
		code.endl();
	}

	public void makeClassDeclaration(CodeWriter clsCode) {
		AccessInfo af = cls.getAccessFlags();
		if (af.isInterface()) {
			af = af.remove(AccessFlags.ACC_ABSTRACT);
		} else if (af.isEnum()) {
			af = af.remove(AccessFlags.ACC_FINAL).remove(AccessFlags.ACC_ABSTRACT);
		}

		annotationGen.addForClass(clsCode);
		clsCode.startLine(af.makeString());
		if (af.isInterface()) {
			if (af.isAnnotation())
				clsCode.add('@');
			clsCode.add("interface ");
		} else if (af.isEnum()) {
			clsCode.add("enum ");
		} else {
			clsCode.add("class ");
		}
		clsCode.add(cls.getShortName());
		ClassInfo sup = cls.getSuperClass();

		if (sup != null
				&& !sup.getFullName().equals(Consts.CLASS_OBJECT)
				&& !sup.getFullName().equals(Consts.CLASS_ENUM)) {
			clsCode.add(" extends ").add(useClass(sup));
		}

		if (cls.getInterfaces().size() > 0 && !af.isAnnotation()) {
			if (cls.getAccessFlags().isInterface())
				clsCode.add(" extends ");
			else
				clsCode.add(" implements ");

			for (Iterator<ClassInfo> it = cls.getInterfaces().iterator(); it.hasNext();) {
				ClassInfo interf = it.next();
				clsCode.add(useClass(interf));
				if (it.hasNext())
					clsCode.add(", ");
			}
		}
	}

	public void makeClassBody(CodeWriter clsCode) throws CodegenException {
		clsCode.add(" {");
		CodeWriter mthsCode = makeMethods(clsCode, cls.getMethods());
		clsCode.add(makeFields(clsCode, cls, cls.getFields()));

		// insert inner classes code
		if (cls.getInnerClasses().size() != 0) {
			clsCode.add(makeInnerClasses(cls, clsCode.getIndent()));
		}
		clsCode.add(mthsCode);
		clsCode.startLine("}");
	}

	private CodeWriter makeInnerClasses(ClassNode cls2, int indent) throws CodegenException {
		CodeWriter innerClsCode = new CodeWriter(indent + 1);
		for (ClassNode inCls : cls.getInnerClasses()) {
			if (inCls.isAnonymous())
				continue;

			ClassGen inClGen = new ClassGen(inCls, parentGen == null ? this : parentGen, fallback);
			inClGen.addClassCode(innerClsCode);
			imports.addAll(inClGen.getImports());
		}
		return innerClsCode;
	}

	private CodeWriter makeMethods(CodeWriter clsCode, List<MethodNode> mthList) throws CodegenException {
		CodeWriter code = new CodeWriter(clsCode.getIndent() + 1);
		for (Iterator<MethodNode> it = mthList.iterator(); it.hasNext();) {
			MethodNode mth = it.next();
			try {
				if (mth.getAccessFlags().isAbstract() || mth.getAccessFlags().isNative()) {
					MethodGen mthGen = new MethodGen(this, mth);
					mthGen.addDefinition(code);
					if (cls.getAccessFlags().isAnnotation()) {
						Object def = annotationGen.getAnnotationDefaultValue(mth.getName());
						if (def != null) {
							String v = annotationGen.encValueToString(def);
							code.add(" default ").add(v);
						}
					}
					code.add(";");
				} else {
					if (mth.isNoCode())
						continue;

					MethodGen mthGen = new MethodGen(this, mth);
					mthGen.addDefinition(code);
					code.add(" {");
					code.add(mthGen.makeInstructions(code.getIndent()));
					code.startLine("}");
				}
			} catch (Throwable e) {
				String msg = ErrorsCounter.methodError(mth, "Method generation error", e);
				code.startLine("/* " + msg + CodeWriter.NL + Utils.getStackTrace(e) + "*/");
			}

			if (it.hasNext())
				code.endl();
		}
		return code;
	}

	private CodeWriter makeFields(CodeWriter clsCode, ClassNode cls, List<FieldNode> fields) throws CodegenException {
		CodeWriter code = new CodeWriter(clsCode.getIndent() + 1);

		EnumClassAttr enumFields = (EnumClassAttr) cls.getAttributes().get(AttributeType.ENUM_CLASS);
		if (enumFields != null) {
			MethodGen mthGen = new MethodGen(this, enumFields.getStaticMethod());
			InsnGen igen = new InsnGen(mthGen, enumFields.getStaticMethod(), false);

			for (Iterator<EnumField> it = enumFields.getFields().iterator(); it.hasNext();) {
				EnumField f = it.next();
				code.startLine(f.getName());
				if (f.getArgs().size() != 0) {
					code.add('(');
					for (Iterator<InsnArg> aIt = f.getArgs().iterator(); aIt.hasNext();) {
						InsnArg arg = aIt.next();
						code.add(igen.arg(arg));
						if (aIt.hasNext())
							code.add(", ");
					}
					code.add(')');
				}
				if (f.getCls() != null) {
					new ClassGen(f.getCls(), this, fallback).makeClassBody(code);
				}
				if (it.hasNext())
					code.add(',');
			}
			if (enumFields.getFields().isEmpty())
				code.startLine();

			code.add(';');
			code.endl();
		}

		for (FieldNode f : fields) {
			annotationGen.addForField(code, f);
			code.startLine(f.getAccessFlags().makeString());
			code.add(TypeGen.translate(this, f.getType()));
			code.add(" ");
			code.add(f.getName());
			FieldValueAttr fv = (FieldValueAttr) f.getAttributes().get(AttributeType.FIELD_VALUE);
			if (fv != null) {
				code.add(" = ");
				if (fv.getValue() == null) {
					code.add(TypeGen.literalToString(0, f.getType()));
				} else {
					code.add(annotationGen.encValueToString(fv.getValue()));
				}
			}
			code.add(";");
		}
		if (fields.size() != 0)
			code.endl();
		return code;
	}

	public String useClass(ArgType clsType) {
		return useClass(ClassInfo.fromType(cls.dex(), clsType));
	}

	public String useClass(ClassInfo classInfo) {
		if (parentGen != null)
			return parentGen.useClass(classInfo);

		String clsStr = classInfo.getFullName();
		if (fallback)
			return clsStr;

		String shortName = classInfo.getShortName();

		if (classInfo.getPackage().equals("java.lang") && classInfo.getParentClass() == null) {
			return shortName;
		} else {
			// don't add import if this class inner for current class
			if (isInner(classInfo, cls.getClassInfo()))
				return shortName;

			for (ClassInfo cls : imports) {
				if (!cls.equals(classInfo)) {
					if (cls.getShortName().equals(shortName))
						return clsStr;
				}
			}
			imports.add(classInfo);
			return shortName;
		}
	}

	private boolean isInner(ClassInfo inner, ClassInfo parent) {
		if (inner.isInner()) {
			ClassInfo p = inner.getParentClass();
			return p.equals(parent) || isInner(p, parent);
		}
		return false;
	}

	public Set<ClassInfo> getImports() {
		return imports;
	}

	public ClassGen getParentGen() {
		return parentGen;
	}

	public AnnotationGen getAnnotationGen() {
		return annotationGen;
	}

	public boolean isFallbackMode() {
		return fallback;
	}
}
