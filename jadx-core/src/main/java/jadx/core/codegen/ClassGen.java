package jadx.core.codegen;

import jadx.core.Consts;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.EnumClassAttr;
import jadx.core.dex.attributes.EnumClassAttr.EnumField;
import jadx.core.dex.attributes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.parser.FieldValueAttr;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.CodegenException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.rop.code.AccessFlags;

public class ClassGen {
	private static final Logger LOG = LoggerFactory.getLogger(ClassGen.class);

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
			clsCode.add("package ").add(cls.getPackage()).add(';');
			clsCode.newLine();
		}

		int importsCount = imports.size();
		if (importsCount != 0) {
			List<String> sortImports = new ArrayList<String>(importsCount);
			for (ClassInfo ic : imports) {
				sortImports.add(ic.getFullName());
			}
			Collections.sort(sortImports);

			for (String imp : sortImports) {
				clsCode.startLine("import ").add(imp).add(';');
			}
			clsCode.newLine();

			sortImports.clear();
			imports.clear();
		}

		clsCode.add(clsBody);
		return clsCode;
	}

	public void addClassCode(CodeWriter code) throws CodegenException {
		if (cls.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
			return;
		}
		if (cls.getAttributes().contains(AttributeFlag.INCONSISTENT_CODE)) {
			code.startLine("// jadx: inconsistent code");
		}
		makeClassDeclaration(code);
		makeClassBody(code);
		code.newLine();
	}

	public void makeClassDeclaration(CodeWriter clsCode) {
		AccessInfo af = cls.getAccessFlags();
		if (af.isInterface()) {
			af = af.remove(AccessFlags.ACC_ABSTRACT);
		} else if (af.isEnum()) {
			af = af.remove(AccessFlags.ACC_FINAL).remove(AccessFlags.ACC_ABSTRACT);
		}

		annotationGen.addForClass(clsCode);
		insertSourceFileInfo(clsCode, cls);
		clsCode.startLine(af.makeString());
		if (af.isInterface()) {
			if (af.isAnnotation()) {
				clsCode.add('@');
			}
			clsCode.add("interface ");
		} else if (af.isEnum()) {
			clsCode.add("enum ");
		} else {
			clsCode.add("class ");
		}
		clsCode.add(cls.getShortName());

		makeGenericMap(clsCode, cls.getGenericMap());
		clsCode.add(' ');

		ClassInfo sup = cls.getSuperClass();
		if (sup != null
				&& !sup.getFullName().equals(Consts.CLASS_OBJECT)
				&& !sup.getFullName().equals(Consts.CLASS_ENUM)) {
			clsCode.add("extends ").add(useClass(sup)).add(' ');
		}

		if (cls.getInterfaces().size() > 0 && !af.isAnnotation()) {
			if (cls.getAccessFlags().isInterface()) {
				clsCode.add("extends ");
			} else {
				clsCode.add("implements ");
			}
			for (Iterator<ClassInfo> it = cls.getInterfaces().iterator(); it.hasNext(); ) {
				ClassInfo interf = it.next();
				clsCode.add(useClass(interf));
				if (it.hasNext()) {
					clsCode.add(", ");
				}
			}
			if (!cls.getInterfaces().isEmpty()) {
				clsCode.add(' ');
			}
		}

		clsCode.attachAnnotation(cls);
	}

	public boolean makeGenericMap(CodeWriter code, Map<ArgType, List<ArgType>> gmap) {
		if (gmap == null || gmap.isEmpty()) {
			return false;
		}
		code.add('<');
		int i = 0;
		for (Entry<ArgType, List<ArgType>> e : gmap.entrySet()) {
			ArgType type = e.getKey();
			List<ArgType> list = e.getValue();
			if (i != 0) {
				code.add(", ");
			}
			code.add(useClass(type));
			if (list != null && !list.isEmpty()) {
				code.add(" extends ");
				for (Iterator<ArgType> it = list.iterator(); it.hasNext(); ) {
					ArgType g = it.next();
					code.add(useClass(g));
					if (it.hasNext()) {
						code.add(" & ");
					}
				}
			}
			i++;
		}
		code.add('>');
		return true;
	}

	public void makeClassBody(CodeWriter clsCode) throws CodegenException {
		clsCode.add('{');
		CodeWriter mthsCode = makeMethods(clsCode, cls.getMethods());
		CodeWriter fieldsCode = makeFields(clsCode, cls, cls.getFields());
		clsCode.add(fieldsCode);
		if (fieldsCode.notEmpty() && mthsCode.notEmpty()) {
			clsCode.newLine();
		}
		// insert inner classes code
		if (cls.getInnerClasses().size() != 0) {
			clsCode.add(makeInnerClasses(cls, clsCode.getIndent()));
			if (mthsCode.notEmpty()) {
				clsCode.newLine();
			}
		}
		clsCode.add(mthsCode);
		clsCode.startLine('}');
	}

	private CodeWriter makeInnerClasses(ClassNode cls, int indent) throws CodegenException {
		CodeWriter innerClsCode = new CodeWriter(indent + 1);
		for (ClassNode inCls : cls.getInnerClasses()) {
			if (!inCls.isAnonymous()) {
				ClassGen inClGen = new ClassGen(inCls, parentGen == null ? this : parentGen, fallback);
				inClGen.addClassCode(innerClsCode);
				imports.addAll(inClGen.getImports());
			}
		}
		return innerClsCode;
	}

	private CodeWriter makeMethods(CodeWriter clsCode, List<MethodNode> mthList) {
		CodeWriter code = new CodeWriter(clsCode.getIndent() + 1);
		for (Iterator<MethodNode> it = mthList.iterator(); it.hasNext(); ) {
			MethodNode mth = it.next();
			if (mth.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
				continue;
			}
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
					code.add(';');
				} else {
					MethodGen mthGen = new MethodGen(this, mth);
					boolean badCode = mth.getAttributes().contains(AttributeFlag.INCONSISTENT_CODE);
					if (badCode) {
						code.startLine("/* JADX WARNING: inconsistent code. */");
						code.startLine("/* Code decompiled incorrectly, please refer to instructions dump. */");
						LOG.error(ErrorsCounter.formatErrorMsg(mth, " Inconsistent code"));
					}
					if (mthGen.addDefinition(code)) {
						code.add(' ');
					}
					code.add('{');
					code.incIndent();
					insertSourceFileInfo(code, mth);
					mthGen.addInstructions(code);
					code.decIndent();
					code.startLine('}');
				}
			} catch (Throwable e) {
				String msg = ErrorsCounter.methodError(mth, "Method generation error", e);
				code.startLine("/* " + msg + CodeWriter.NL + Utils.getStackTrace(e) + " */");
			}
			if (it.hasNext()) {
				code.newLine();
			}
		}
		return code;
	}

	private CodeWriter makeFields(CodeWriter clsCode, ClassNode cls, List<FieldNode> fields) throws CodegenException {
		CodeWriter code = new CodeWriter(clsCode.getIndent() + 1);

		addEnumFields(cls, code);

		for (FieldNode f : fields) {
			if (f.getAttributes().contains(AttributeFlag.DONT_GENERATE)) {
				continue;
			}
			annotationGen.addForField(code, f);
			code.startLine(f.getAccessFlags().makeString());
			code.add(TypeGen.translate(this, f.getType()));
			code.add(' ');
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
			code.add(';');
			code.attachAnnotation(f);
		}
		return code;
	}

	private void addEnumFields(ClassNode cls, CodeWriter code) throws CodegenException {
		EnumClassAttr enumFields = (EnumClassAttr) cls.getAttributes().get(AttributeType.ENUM_CLASS);
		if (enumFields != null) {
			InsnGen igen = null;
			for (Iterator<EnumField> it = enumFields.getFields().iterator(); it.hasNext(); ) {
				EnumField f = it.next();
				code.startLine(f.getName());
				if (f.getArgs().size() != 0) {
					code.add('(');
					for (Iterator<InsnArg> aIt = f.getArgs().iterator(); aIt.hasNext(); ) {
						InsnArg arg = aIt.next();
						if (igen == null) {
							// don't init mth gen if this is simple enum
							MethodGen mthGen = new MethodGen(this, enumFields.getStaticMethod());
							igen = new InsnGen(mthGen, enumFields.getStaticMethod(), false);
						}
						igen.addArg(code, arg);
						if (aIt.hasNext()) {
							code.add(", ");
						}
					}
					code.add(')');
				}
				if (f.getCls() != null) {
					new ClassGen(f.getCls(), this, fallback).makeClassBody(code);
				}
				if (it.hasNext()) {
					code.add(',');
				}
			}
			if (enumFields.getFields().isEmpty()) {
				code.startLine();
			}
			code.add(';');
			code.newLine();
		}
	}

	public String useClass(ArgType clsType) {
		if (clsType.isGenericType()) {
			return clsType.getObject();
		}
		return useClass(ClassInfo.fromType(clsType));
	}

	public String useClass(ClassInfo classInfo) {
		String baseClass = useClassInternal(cls.getClassInfo(), classInfo);
		ArgType type = classInfo.getType();
		ArgType[] generics = type.getGenericTypes();
		if (generics == null) {
			return baseClass;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(baseClass);
		sb.append('<');
		int len = generics.length;
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				sb.append(", ");
			}
			ArgType gt = generics[i];
			ArgType wt = gt.getWildcardType();
			if (wt != null) {
				sb.append('?');
				int bounds = gt.getWildcardBounds();
				if (bounds != 0) {
					sb.append(bounds == -1 ? " super " : " extends ");
					sb.append(TypeGen.translate(this, wt));
				}
			} else {
				sb.append(TypeGen.translate(this, gt));
			}
		}
		sb.append('>');
		return sb.toString();
	}

	private String useClassInternal(ClassInfo useCls, ClassInfo classInfo) {
		String fullName = classInfo.getFullName();
		if (fallback) {
			return fullName;
		}
		String shortName = classInfo.getShortName();
		if (classInfo.getPackage().equals("java.lang") && classInfo.getParentClass() == null) {
			return shortName;
		} else {
			// don't add import if this class inner for current class
			if (isClassInnerFor(classInfo, useCls)) {
				return shortName;
			}
			// don't add import if this class from same package
			if (classInfo.getPackage().equals(useCls.getPackage()) && !classInfo.isInner()) {
				return shortName;
			}
			// don't add import if class not public (must be accessed using inheritance)
			ClassNode classNode = cls.dex().resolveClass(classInfo);
			if (classNode != null && !classNode.getAccessFlags().isPublic()) {
				return shortName;
			}
			if (searchCollision(cls.dex(), useCls, shortName)) {
				return fullName;
			}
			if (classInfo.getPackage().equals(useCls.getPackage())) {
				fullName = classInfo.getNameWithoutPackage();
			}
			for (ClassInfo importCls : getImports()) {
				if (!importCls.equals(classInfo)
						&& importCls.getShortName().equals(shortName)) {
					if (classInfo.isInner()) {
						String parent = useClassInternal(useCls, classInfo.getParentClass());
						return parent + "." + shortName;
					} else {
						return fullName;
					}
				}
			}
			addImport(classInfo);
			return shortName;
		}
	}

	private void addImport(ClassInfo classInfo) {
		if (parentGen != null) {
			parentGen.addImport(classInfo);
		} else {
			imports.add(classInfo);
		}
	}

	private Set<ClassInfo> getImports() {
		if (parentGen != null) {
			return parentGen.getImports();
		} else {
			return imports;
		}
	}

	private static boolean isClassInnerFor(ClassInfo inner, ClassInfo parent) {
		if (inner.isInner()) {
			ClassInfo p = inner.getParentClass();
			return p.equals(parent) || isClassInnerFor(p, parent);
		}
		return false;
	}

	private static boolean searchCollision(DexNode dex, ClassInfo useCls, String shortName) {
		if (useCls == null) {
			return false;
		}
		if (useCls.getShortName().equals(shortName)) {
			return true;
		}
		ClassNode classNode = dex.resolveClass(useCls);
		if (classNode != null) {
			for (ClassNode inner : classNode.getInnerClasses()) {
				if (inner.getShortName().equals(shortName)) {
					return true;
				}
			}
		}
		return searchCollision(dex, useCls.getParentClass(), shortName);
	}

	private void insertSourceFileInfo(CodeWriter code, AttrNode node) {
		SourceFileAttr sourceFileAttr = (SourceFileAttr) node.getAttributes().get(AttributeType.SOURCE_FILE);
		if (sourceFileAttr != null) {
			code.startLine("// compiled from: ").add(sourceFileAttr.getFileName());
		}
	}

	public ClassGen getParentGen() {
		return parentGen == null ? this : parentGen;
	}

	public AnnotationGen getAnnotationGen() {
		return annotationGen;
	}

	public boolean isFallbackMode() {
		return fallback;
	}
}
