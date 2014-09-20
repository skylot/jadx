package jadx.core.codegen;

import jadx.api.IJadxArgs;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr.EnumField;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
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

	private boolean showInconsistentCode = false;

	private final Set<ClassInfo> imports = new HashSet<ClassInfo>();
	private int clsDeclLine;

	public ClassGen(ClassNode cls, ClassGen parentClsGen, IJadxArgs jadxArgs) {
		this(cls, parentClsGen, jadxArgs.isFallbackMode());
		this.showInconsistentCode = jadxArgs.isShowInconsistentCode();
	}

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
		if (cls.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		if (cls.contains(AFlag.INCONSISTENT_CODE)) {
			code.startLine("// jadx: inconsistent code");
		}
		addClassDeclaration(code);
		addClassBody(code);
	}

	public void addClassDeclaration(CodeWriter clsCode) {
		AccessInfo af = cls.getAccessFlags();
		if (af.isInterface()) {
			af = af.remove(AccessFlags.ACC_ABSTRACT);
		} else if (af.isEnum()) {
			af = af.remove(AccessFlags.ACC_FINAL)
					.remove(AccessFlags.ACC_ABSTRACT)
					.remove(AccessFlags.ACC_STATIC);
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

		addGenericMap(clsCode, cls.getGenericMap());
		clsCode.add(' ');

		ClassInfo sup = cls.getSuperClass();
		if (sup != null
				&& !sup.getFullName().equals(Consts.CLASS_OBJECT)
				&& !sup.getFullName().equals(Consts.CLASS_ENUM)) {
			clsCode.add("extends ");
			useClass(clsCode, sup);
			clsCode.add(' ');
		}

		if (cls.getInterfaces().size() > 0 && !af.isAnnotation()) {
			if (cls.getAccessFlags().isInterface()) {
				clsCode.add("extends ");
			} else {
				clsCode.add("implements ");
			}
			for (Iterator<ClassInfo> it = cls.getInterfaces().iterator(); it.hasNext(); ) {
				ClassInfo interf = it.next();
				useClass(clsCode, interf);
				if (it.hasNext()) {
					clsCode.add(", ");
				}
			}
			if (!cls.getInterfaces().isEmpty()) {
				clsCode.add(' ');
			}
		}
		clsCode.attachDefinition(cls);
	}

	public boolean addGenericMap(CodeWriter code, Map<ArgType, List<ArgType>> gmap) {
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
			if (type.isGenericType()) {
				code.add(type.getObject());
			} else {
				useClass(code, ClassInfo.fromType(type));
			}
			if (list != null && !list.isEmpty()) {
				code.add(" extends ");
				for (Iterator<ArgType> it = list.iterator(); it.hasNext(); ) {
					ArgType g = it.next();
					if (g.isGenericType()) {
						code.add(g.getObject());
					} else {
						useClass(code, ClassInfo.fromType(g));
					}
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

	public void addClassBody(CodeWriter clsCode) throws CodegenException {
		clsCode.add('{');
		clsDeclLine = clsCode.getLine();
		clsCode.incIndent();
		addFields(clsCode);
		addInnerClasses(clsCode, cls);
		addMethods(clsCode);
		clsCode.decIndent();
		clsCode.startLine('}');
	}

	private void addInnerClasses(CodeWriter code, ClassNode cls) throws CodegenException {
		for (ClassNode innerCls : cls.getInnerClasses()) {
			if (innerCls.contains(AFlag.DONT_GENERATE)
					|| innerCls.isAnonymous()) {
				continue;
			}
			ClassGen inClGen = new ClassGen(innerCls, getParentGen(), fallback);
			code.newLine();
			inClGen.addClassCode(code);
			imports.addAll(inClGen.getImports());
		}
	}

	private boolean isInnerClassesPresents() {
		for (ClassNode innerCls : cls.getInnerClasses()) {
			if (!innerCls.isAnonymous()) {
				return true;
			}
		}
		return false;
	}

	private void addMethods(CodeWriter code) {
		for (MethodNode mth : cls.getMethods()) {
			if (mth.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (code.getLine() != clsDeclLine) {
				code.newLine();
			}
			try {
				addMethod(code, mth);
			} catch (Exception e) {
				String msg = ErrorsCounter.methodError(mth, "Method generation error", e);
				code.startLine("/* " + msg + CodeWriter.NL + Utils.getStackTrace(e) + " */");
			}
		}
	}

	private boolean isMethodsPresents() {
		for (MethodNode mth : cls.getMethods()) {
			if (!mth.contains(AFlag.DONT_GENERATE)) {
				return true;
			}
		}
		return false;
	}

	private void addMethod(CodeWriter code, MethodNode mth) throws CodegenException {
		if (mth.getAccessFlags().isAbstract() || mth.getAccessFlags().isNative()) {
			MethodGen mthGen = new MethodGen(this, mth);
			mthGen.addDefinition(code);
			if (cls.getAccessFlags().isAnnotation()) {
				Object def = annotationGen.getAnnotationDefaultValue(mth.getName());
				if (def != null) {
					code.add(" default ");
					annotationGen.encodeValue(code, def);
				}
			}
			code.add(';');
		} else {
			boolean badCode = mth.contains(AFlag.INCONSISTENT_CODE);
			if (badCode) {
				code.startLine("/* JADX WARNING: inconsistent code. */");
				code.startLine("/* Code decompiled incorrectly, please refer to instructions dump. */");
				ErrorsCounter.methodError(mth, "Inconsistent code");
				if (showInconsistentCode) {
					mth.remove(AFlag.INCONSISTENT_CODE);
				}
			}
			MethodGen mthGen;
			if (badCode || mth.contains(AType.JADX_ERROR)) {
				mthGen = MethodGen.getFallbackMethodGen(mth);
			} else {
				mthGen = new MethodGen(this, mth);
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
	}

	private void addFields(CodeWriter code) throws CodegenException {
		addEnumFields(code);
		for (FieldNode f : cls.getFields()) {
			if (f.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			annotationGen.addForField(code, f);
			code.startLine(f.getAccessFlags().makeString());
			useType(code, f.getType());
			code.add(' ');
			code.add(f.getName());
			FieldValueAttr fv = f.get(AType.FIELD_VALUE);
			if (fv != null) {
				code.add(" = ");
				if (fv.getValue() == null) {
					code.add(TypeGen.literalToString(0, f.getType()));
				} else {
					annotationGen.encodeValue(code, fv.getValue());
				}
			}
			code.add(';');
			code.attachDefinition(f);
		}
	}

	private boolean isFieldsPresents() {
		for (FieldNode field : cls.getFields()) {
			if (!field.contains(AFlag.DONT_GENERATE)) {
				return true;
			}
		}
		return false;
	}

	private void addEnumFields(CodeWriter code) throws CodegenException {
		EnumClassAttr enumFields = cls.get(AType.ENUM_CLASS);
		if (enumFields == null) {
			return;
		}
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
						igen = new InsnGen(mthGen, false);
					}
					igen.addArg(code, arg);
					if (aIt.hasNext()) {
						code.add(", ");
					}
				}
				code.add(')');
			}
			if (f.getCls() != null) {
				code.add(' ');
				new ClassGen(f.getCls(), this, fallback).addClassBody(code);
			}
			if (it.hasNext()) {
				code.add(',');
			}
		}
		if (isMethodsPresents() || isFieldsPresents() || isInnerClassesPresents()) {
			if (enumFields.getFields().isEmpty()) {
				code.startLine();
			}
			code.add(';');
		}
	}

	public void useType(CodeWriter code, ArgType type) {
		PrimitiveType stype = type.getPrimitiveType();
		if (stype == null) {
			code.add(type.toString());
		} else if (stype == PrimitiveType.OBJECT) {
			if (type.isGenericType()) {
				code.add(type.getObject());
			} else {
				useClass(code, ClassInfo.fromType(type));
			}
		} else if (stype == PrimitiveType.ARRAY) {
			useType(code, type.getArrayElement());
			code.add("[]");
		} else {
			code.add(stype.getLongName());
		}
	}

	public void useClass(CodeWriter code, ClassInfo classInfo) {
		ClassNode classNode = cls.dex().resolveClass(classInfo);
		if (classNode != null) {
			code.attachAnnotation(classNode);
		}
		String baseClass = useClassInternal(cls.getClassInfo(), classInfo);
		ArgType[] generics = classInfo.getType().getGenericTypes();
		code.add(baseClass);
		if (generics != null) {
			code.add('<');
			int len = generics.length;
			for (int i = 0; i < len; i++) {
				if (i != 0) {
					code.add(", ");
				}
				ArgType gt = generics[i];
				ArgType wt = gt.getWildcardType();
				if (wt != null) {
					code.add('?');
					int bounds = gt.getWildcardBounds();
					if (bounds != 0) {
						code.add(bounds == -1 ? " super " : " extends ");
						useType(code, wt);
					}
				} else {
					useType(code, gt);
				}
			}
			code.add('>');
		}
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
		SourceFileAttr sourceFileAttr = node.get(AType.SOURCE_FILE);
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
