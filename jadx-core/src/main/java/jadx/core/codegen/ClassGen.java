package jadx.core.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.android.dx.rop.code.AccessFlags;

import jadx.api.JadxArgs;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr.EnumField;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.parser.FieldInitAttr;
import jadx.core.dex.nodes.parser.FieldInitAttr.InitType;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.CodegenException;

public class ClassGen {

	private final ClassNode cls;
	private final ClassGen parentGen;
	private final AnnotationGen annotationGen;
	private final boolean fallback;
	private final boolean useImports;
	private final boolean showInconsistentCode;

	private final Set<ClassInfo> imports = new HashSet<>();
	private int clsDeclLine;

	public ClassGen(ClassNode cls, JadxArgs jadxArgs) {
		this(cls, null, jadxArgs.isUseImports(), jadxArgs.isFallbackMode(), jadxArgs.isShowInconsistentCode());
	}

	public ClassGen(ClassNode cls, ClassGen parentClsGen) {
		this(cls, parentClsGen, parentClsGen.useImports, parentClsGen.fallback, parentClsGen.showInconsistentCode);
	}

	public ClassGen(ClassNode cls, ClassGen parentClsGen, boolean useImports, boolean fallback, boolean showBadCode) {
		this.cls = cls;
		this.parentGen = parentClsGen;
		this.fallback = fallback;
		this.useImports = useImports;
		this.showInconsistentCode = showBadCode;

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
			List<String> sortImports = new ArrayList<>(importsCount);
			for (ClassInfo ic : imports) {
				sortImports.add(ic.getAlias().getFullName());
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
			af = af.remove(AccessFlags.ACC_ABSTRACT)
					.remove(AccessFlags.ACC_STATIC);
		} else if (af.isEnum()) {
			af = af.remove(AccessFlags.ACC_FINAL)
					.remove(AccessFlags.ACC_ABSTRACT)
					.remove(AccessFlags.ACC_STATIC);
		}

		// 'static' and 'private' modifier not allowed for top classes (not inner)
		if (!cls.getAlias().isInner()) {
			af = af.remove(AccessFlags.ACC_STATIC).remove(AccessFlags.ACC_PRIVATE);
		}

		annotationGen.addForClass(clsCode);
		insertSourceFileInfo(clsCode, cls);
		insertRenameInfo(clsCode, cls);
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
		clsCode.attachDefinition(cls);
		clsCode.add(cls.getShortName());

		addGenericMap(clsCode, cls.getGenericMap());
		clsCode.add(' ');

		ArgType sup = cls.getSuperClass();
		if (sup != null
				&& !sup.equals(ArgType.OBJECT)
				&& !sup.getObject().equals(ArgType.ENUM.getObject())) {
			clsCode.add("extends ");
			useClass(clsCode, sup);
			clsCode.add(' ');
		}

		if (!cls.getInterfaces().isEmpty() && !af.isAnnotation()) {
			if (cls.getAccessFlags().isInterface()) {
				clsCode.add("extends ");
			} else {
				clsCode.add("implements ");
			}
			for (Iterator<ArgType> it = cls.getInterfaces().iterator(); it.hasNext(); ) {
				ArgType interf = it.next();
				useClass(clsCode, interf);
				if (it.hasNext()) {
					clsCode.add(", ");
				}
			}
			if (!cls.getInterfaces().isEmpty()) {
				clsCode.add(' ');
			}
		}
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
				useClass(code, type);
			}
			if (list != null && !list.isEmpty()) {
				code.add(" extends ");
				for (Iterator<ArgType> it = list.iterator(); it.hasNext(); ) {
					ArgType g = it.next();
					if (g.isGenericType()) {
						code.add(g.getObject());
					} else {
						useClass(code, g);
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
					|| innerCls.contains(AFlag.ANONYMOUS_CLASS)) {
				continue;
			}
			ClassGen inClGen = new ClassGen(innerCls, getParentGen());
			code.newLine();
			inClGen.addClassCode(code);
			imports.addAll(inClGen.getImports());
		}
	}

	private boolean isInnerClassesPresents() {
		for (ClassNode innerCls : cls.getInnerClasses()) {
			if (!innerCls.contains(AFlag.ANONYMOUS_CLASS)) {
				return true;
			}
		}
		return false;
	}

	private void addMethods(CodeWriter code) {
		List<MethodNode> methods = sortMethodsByLine(cls.getMethods());
		for (MethodNode mth : methods) {
			if (mth.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (code.getLine() != clsDeclLine) {
				code.newLine();
			}
			try {
				addMethod(code, mth);
			} catch (Exception e) {
				code.newLine().add("/*");
				code.newLine().add(ErrorsCounter.methodError(mth, "Method generation error", e));
				code.newLine().add(Utils.getStackTrace(e));
				code.newLine().add("*/");
			}
		}
	}

	private static List<MethodNode> sortMethodsByLine(List<MethodNode> methods) {
		List<MethodNode> out = new ArrayList<>(methods);
		out.sort(Comparator.comparingInt(LineAttrNode::getSourceLine));
		return out;
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
					badCode = false;
				}
			}
			MethodGen mthGen;
			if (badCode || mth.contains(AType.JADX_ERROR) || fallback) {
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
			if (fallback) {
				mthGen.addFallbackMethodCode(code);
			} else {
				mthGen.addInstructions(code);
			}
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

			if (f.getFieldInfo().isRenamed()) {
				code.startLine("/* renamed from: ").add(f.getName()).add(" */");
			}
			code.startLine(f.getAccessFlags().makeString());
			useType(code, f.getType());
			code.add(' ');
			code.attachDefinition(f);
			code.add(f.getAlias());
			FieldInitAttr fv = f.get(AType.FIELD_INIT);
			if (fv != null) {
				code.add(" = ");
				if (fv.getValue() == null) {
					code.add(TypeGen.literalToString(0, f.getType(), cls));
				} else {
					if (fv.getValueType() == InitType.CONST) {
						annotationGen.encodeValue(code, fv.getValue());
					} else if (fv.getValueType() == InitType.INSN) {
						InsnGen insnGen = makeInsnGen(fv.getInsnMth());
						addInsnBody(insnGen, code, fv.getInsn());
					}
				}
			}
			code.add(';');
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
			code.startLine(f.getField().getAlias());
			ConstructorInsn constrInsn = f.getConstrInsn();
			if (constrInsn.getArgsCount() > f.getStartArg()) {
				if (igen == null) {
					igen = makeInsnGen(enumFields.getStaticMethod());
				}
				MethodNode callMth = cls.dex().resolveMethod(constrInsn.getCallMth());
				igen.generateMethodArguments(code, constrInsn, f.getStartArg(), callMth);
			}
			if (f.getCls() != null) {
				code.add(' ');
				new ClassGen(f.getCls(), this).addClassBody(code);
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
			if (isFieldsPresents()) {
				code.startLine();
			}
		}
	}

	private InsnGen makeInsnGen(MethodNode mth) {
		MethodGen mthGen = new MethodGen(this, mth);
		return new InsnGen(mthGen, false);
	}

	private void addInsnBody(InsnGen insnGen, CodeWriter code, InsnNode insn) {
		try {
			insnGen.makeInsn(insn, code, InsnGen.Flags.BODY_ONLY_NOWRAP);
		} catch (Exception e) {
			ErrorsCounter.classError(cls, "Failed to generate init code", e);
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
				useClass(code, type);
			}
		} else if (stype == PrimitiveType.ARRAY) {
			useType(code, type.getArrayElement());
			code.add("[]");
		} else {
			code.add(stype.getLongName());
		}
	}

	public void useClass(CodeWriter code, ArgType type) {
		useClass(code, ClassInfo.extCls(cls.root(), type));
		ArgType[] generics = type.getGenericTypes();
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

	public void useClass(CodeWriter code, ClassInfo classInfo) {
		ClassNode classNode = cls.dex().resolveClass(classInfo);
		if (classNode != null) {
			code.attachAnnotation(classNode);
		}
		String baseClass = useClassInternal(cls.getAlias(), classInfo.getAlias());
		code.add(baseClass);
	}

	private String useClassInternal(ClassInfo useCls, ClassInfo extClsInfo) {
		String fullName = extClsInfo.getFullName();
		if (fallback || !useImports) {
			return fullName;
		}
		String shortName = extClsInfo.getShortName();
		if (extClsInfo.getPackage().equals("java.lang") && extClsInfo.getParentClass() == null) {
			return shortName;
		}
		if (isClassInnerFor(useCls, extClsInfo)) {
			return shortName;
		}
		if (isBothClassesInOneTopClass(useCls, extClsInfo)) {
			return shortName;
		}
		// don't add import if this class from same package
		if (extClsInfo.getPackage().equals(useCls.getPackage()) && !extClsInfo.isInner()) {
			return shortName;
		}
		// don't add import if class not public (must be accessed using inheritance)
		ClassNode classNode = cls.dex().resolveClass(extClsInfo);
		if (classNode != null && !classNode.getAccessFlags().isPublic()) {
			return shortName;
		}
		if (searchCollision(cls.dex(), useCls, extClsInfo)) {
			return fullName;
		}
		// ignore classes from default package
		if (extClsInfo.isDefaultPackage()) {
			return shortName;
		}
		if (extClsInfo.getPackage().equals(useCls.getPackage())) {
			fullName = extClsInfo.getNameWithoutPackage();
		}
		for (ClassInfo importCls : getImports()) {
			if (!importCls.equals(extClsInfo)
					&& importCls.getShortName().equals(shortName)) {
				if (extClsInfo.isInner()) {
					String parent = useClassInternal(useCls, extClsInfo.getParentClass().getAlias());
					return parent + "." + shortName;
				} else {
					return fullName;
				}
			}
		}
		addImport(extClsInfo);
		return shortName;
	}

	private void addImport(ClassInfo classInfo) {
		if (parentGen != null) {
			parentGen.addImport(classInfo.getAlias());
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

	private static boolean isBothClassesInOneTopClass(ClassInfo useCls, ClassInfo extClsInfo) {
		ClassInfo a = useCls.getTopParentClass();
		ClassInfo b = extClsInfo.getTopParentClass();
		if (a != null) {
			return a.equals(b);
		}
		// useCls - is a top class
		return useCls.equals(b);
	}

	private static boolean isClassInnerFor(ClassInfo inner, ClassInfo parent) {
		if (inner.isInner()) {
			ClassInfo p = inner.getParentClass();
			return p.equals(parent) || isClassInnerFor(p, parent);
		}
		return false;
	}

	private static boolean searchCollision(DexNode dex, ClassInfo useCls, ClassInfo searchCls) {
		if (useCls == null) {
			return false;
		}
		String shortName = searchCls.getShortName();
		if (useCls.getShortName().equals(shortName)) {
			return true;
		}
		ClassNode classNode = dex.resolveClass(useCls);
		if (classNode != null) {
			for (ClassNode inner : classNode.getInnerClasses()) {
				if (inner.getShortName().equals(shortName)
						&& !inner.getAlias().equals(searchCls)) {
					return true;
				}
			}
		}
		return searchCollision(dex, useCls.getParentClass(), searchCls);
	}

	private void insertSourceFileInfo(CodeWriter code, AttrNode node) {
		SourceFileAttr sourceFileAttr = node.get(AType.SOURCE_FILE);
		if (sourceFileAttr != null) {
			code.startLine("/* compiled from: ").add(sourceFileAttr.getFileName()).add(" */");
		}
	}

	private void insertRenameInfo(CodeWriter code, ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		if (classInfo.isRenamed()) {
			code.startLine("/* renamed from: ").add(classInfo.getType().getObject()).add(" */");
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
