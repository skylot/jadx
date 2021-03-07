package jadx.core.codegen;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.data.annotations.InsnCodeOffset;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.GenericInfoAttr;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayInsn;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeCustomNode;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.Named;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.utils.CodeGenUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.VariableNode.VarKind;
import static jadx.core.utils.android.AndroidResourcesUtils.handleAppResField;

public class InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(InsnGen.class);

	protected final MethodGen mgen;
	protected final MethodNode mth;
	protected final RootNode root;
	protected final boolean fallback;

	protected enum Flags {
		BODY_ONLY,
		BODY_ONLY_NOWRAP,
		INLINE
	}

	public InsnGen(MethodGen mgen, boolean fallback) {
		this.mgen = mgen;
		this.mth = mgen.getMethodNode();
		this.root = mth.root();
		this.fallback = fallback;
	}

	private boolean isFallback() {
		return fallback;
	}

	public void addArgDot(ICodeWriter code, InsnArg arg) throws CodegenException {
		int len = code.getLength();
		addArg(code, arg, true);
		if (len != code.getLength()) {
			code.add('.');
		}
	}

	public void addArg(ICodeWriter code, InsnArg arg) throws CodegenException {
		addArg(code, arg, true);
	}

	public void addArg(ICodeWriter code, InsnArg arg, boolean wrap) throws CodegenException {
		if (arg.isRegister()) {
			CodeVar codeVar = CodeGenUtils.getCodeVar((RegisterArg) arg);
			if (codeVar != null) {
				VariableNode node = mth.getVariable(codeVar.getIndex());
				if (node != null) {
					code.attachAnnotation(node);
				}
			}
			code.add(mgen.getNameGen().useArg((RegisterArg) arg));
		} else if (arg.isLiteral()) {
			code.add(lit((LiteralArg) arg));
		} else if (arg.isInsnWrap()) {
			addWrappedArg(code, (InsnWrapArg) arg, wrap);
		} else if (arg.isNamed()) {
			if (arg instanceof NamedArg) {
				VariableNode node = mth.getVariable(((NamedArg) arg).getIndex());
				if (node != null) {
					code.attachAnnotation(node);
				}
			}
			code.add(((Named) arg).getName());
		} else {
			throw new CodegenException("Unknown arg type " + arg);
		}
	}

	private void addWrappedArg(ICodeWriter code, InsnWrapArg arg, boolean wrap) throws CodegenException {
		InsnNode wrapInsn = arg.getWrapInsn();
		if (wrapInsn.contains(AFlag.FORCE_ASSIGN_INLINE)) {
			code.add('(');
			makeInsn(wrapInsn, code, Flags.INLINE);
			code.add(')');
		} else {
			Flags flags = wrap ? Flags.BODY_ONLY : Flags.BODY_ONLY_NOWRAP;
			makeInsn(wrapInsn, code, flags);
		}
	}

	public void assignVar(ICodeWriter code, InsnNode insn) throws CodegenException {
		RegisterArg arg = insn.getResult();
		if (insn.contains(AFlag.DECLARE_VAR)) {
			declareVar(code, arg);
		} else {
			addArg(code, arg, false);
		}
	}

	public void declareVar(ICodeWriter code, RegisterArg arg) {
		declareVar(code, arg.getSVar().getCodeVar());
	}

	public void declareVar(ICodeWriter code, CodeVar codeVar) {
		if (codeVar.isFinal()) {
			code.add("final ");
		}
		useType(code, codeVar.getType());
		code.add(' ');
		VariableNode node = mth.declareVar(codeVar, mgen.getNameGen(), VarKind.VAR);
		String name;
		if (node != null) {
			code.attachDefinition(node);
			name = node.getName();
			codeVar.setName(name);
		} else {
			name = mgen.getNameGen().assignArg(codeVar);
		}
		code.add(name);
	}

	private String lit(LiteralArg arg) {
		return TypeGen.literalToString(arg, mth, fallback);
	}

	private void instanceField(ICodeWriter code, FieldInfo field, InsnArg arg) throws CodegenException {
		ClassNode pCls = mth.getParentClass();
		FieldNode fieldNode = pCls.root().deepResolveField(field);
		if (fieldNode != null) {
			FieldReplaceAttr replace = fieldNode.get(AType.FIELD_REPLACE);
			if (replace != null) {
				switch (replace.getReplaceType()) {
					case CLASS_INSTANCE:
						useClass(code, replace.getClsRef());
						code.add(".this");
						break;
					case VAR:
						addArg(code, replace.getVarRef());
						break;
				}
				return;
			}
		}
		addArgDot(code, arg);
		if (fieldNode != null) {
			code.attachAnnotation(fieldNode);
		}
		if (fieldNode == null) {
			code.add(field.getAlias());
		} else {
			code.add(fieldNode.getAlias());
		}
	}

	public static void makeStaticFieldAccess(ICodeWriter code, FieldInfo field, ClassGen clsGen) {
		ClassInfo declClass = field.getDeclClass();
		// TODO
		boolean fieldFromThisClass = clsGen.getClassNode().getClassInfo().equals(declClass);
		if (!fieldFromThisClass || !clsGen.isBodyGenStarted()) {
			// Android specific resources class handler
			if (!handleAppResField(code, clsGen, declClass)) {
				clsGen.useClass(code, declClass);
			}
			code.add('.');
		}
		FieldNode fieldNode = clsGen.getClassNode().root().deepResolveField(field);
		if (fieldNode != null) {
			code.attachAnnotation(fieldNode);
		}
		if (fieldNode == null) {
			code.add(field.getAlias());
		} else {
			code.add(fieldNode.getAlias());
		}
	}

	protected void staticField(ICodeWriter code, FieldInfo field) {
		makeStaticFieldAccess(code, field, mgen.getClassGen());
	}

	public void useClass(ICodeWriter code, ArgType type) {
		mgen.getClassGen().useClass(code, type);
	}

	public void useClass(ICodeWriter code, ClassInfo cls) {
		mgen.getClassGen().useClass(code, cls);
	}

	protected void useType(ICodeWriter code, ArgType type) {
		mgen.getClassGen().useType(code, type);
	}

	public void makeInsn(InsnNode insn, ICodeWriter code) throws CodegenException {
		makeInsn(insn, code, null);
	}

	private static final Set<Flags> EMPTY_FLAGS = EnumSet.noneOf(Flags.class);
	private static final Set<Flags> BODY_ONLY_FLAG = EnumSet.of(Flags.BODY_ONLY);
	private static final Set<Flags> BODY_ONLY_NOWRAP_FLAGS = EnumSet.of(Flags.BODY_ONLY_NOWRAP);

	protected void makeInsn(InsnNode insn, ICodeWriter code, Flags flag) throws CodegenException {
		if (insn.getType() == InsnType.REGION_ARG) {
			return;
		}
		try {
			if (flag == Flags.BODY_ONLY || flag == Flags.BODY_ONLY_NOWRAP) {
				makeInsnBody(code, insn, flag == Flags.BODY_ONLY ? BODY_ONLY_FLAG : BODY_ONLY_NOWRAP_FLAGS);
			} else {
				if (flag != Flags.INLINE) {
					code.startLineWithNum(insn.getSourceLine());
					InsnCodeOffset.attach(code, insn);
					if (insn.contains(AFlag.COMMENT_OUT)) {
						code.add("// ");
					}
				}
				RegisterArg resArg = insn.getResult();
				if (resArg != null) {
					SSAVar var = resArg.getSVar();
					if (var == null || var.getUseCount() != 0 || insn.getType() != InsnType.CONSTRUCTOR) {
						assignVar(code, insn);
						code.add(" = ");
					}
				}
				makeInsnBody(code, insn, EMPTY_FLAGS);
				if (flag != Flags.INLINE) {
					code.add(';');
					CodeGenUtils.addCodeComments(code, insn);
				}
			}
		} catch (Exception e) {
			throw new CodegenException(mth, "Error generate insn: " + insn, e);
		}
	}

	private void makeInsnBody(ICodeWriter code, InsnNode insn, Set<Flags> state) throws CodegenException {
		switch (insn.getType()) {
			case CONST_STR:
				String str = ((ConstStringNode) insn).getString();
				code.add(mth.root().getStringUtils().unescapeString(str));
				break;

			case CONST_CLASS:
				ArgType clsType = ((ConstClassNode) insn).getClsType();
				useType(code, clsType);
				code.add(".class");
				break;

			case CONST:
				LiteralArg arg = (LiteralArg) insn.getArg(0);
				code.add(lit(arg));
				break;

			case MOVE:
				addArg(code, insn.getArg(0), false);
				break;

			case CHECK_CAST:
			case CAST: {
				boolean wrap = state.contains(Flags.BODY_ONLY);
				if (wrap) {
					code.add('(');
				}
				code.add('(');
				useType(code, (ArgType) ((IndexInsnNode) insn).getIndex());
				code.add(") ");
				addArg(code, insn.getArg(0), true);
				if (wrap) {
					code.add(')');
				}
				break;
			}

			case ARITH:
				makeArith((ArithNode) insn, code, state);
				break;

			case NEG:
				oneArgInsn(code, insn, state, '-');
				break;

			case NOT:
				char op = insn.getArg(0).getType() == ArgType.BOOLEAN ? '!' : '~';
				oneArgInsn(code, insn, state, op);
				break;

			case RETURN:
				if (insn.getArgsCount() != 0) {
					code.add("return ");
					addArg(code, insn.getArg(0), false);
				} else {
					code.add("return");
				}
				break;

			case BREAK:
				code.add("break");
				LoopLabelAttr labelAttr = insn.get(AType.LOOP_LABEL);
				if (labelAttr != null) {
					code.add(' ').add(mgen.getNameGen().getLoopLabel(labelAttr));
				}
				break;

			case CONTINUE:
				code.add("continue");
				break;

			case THROW:
				code.add("throw ");
				addArg(code, insn.getArg(0), true);
				break;

			case CMP_L:
			case CMP_G:
				code.add('(');
				addArg(code, insn.getArg(0));
				code.add(" > ");
				addArg(code, insn.getArg(1));
				code.add(" ? 1 : (");
				addArg(code, insn.getArg(0));
				code.add(" == ");
				addArg(code, insn.getArg(1));
				code.add(" ? 0 : -1))");
				break;

			case INSTANCE_OF: {
				boolean wrap = state.contains(Flags.BODY_ONLY);
				if (wrap) {
					code.add('(');
				}
				addArg(code, insn.getArg(0));
				code.add(" instanceof ");
				useType(code, (ArgType) ((IndexInsnNode) insn).getIndex());
				if (wrap) {
					code.add(')');
				}
				break;
			}
			case CONSTRUCTOR:
				makeConstructor((ConstructorInsn) insn, code);
				break;

			case INVOKE:
				makeInvoke((InvokeNode) insn, code);
				break;

			case NEW_ARRAY: {
				ArgType arrayType = ((NewArrayNode) insn).getArrayType();
				code.add("new ");
				useType(code, arrayType.getArrayRootElement());
				code.add('[');
				addArg(code, insn.getArg(0));
				code.add(']');
				int dim = arrayType.getArrayDimension();
				for (int i = 0; i < dim - 1; i++) {
					code.add("[]");
				}
				break;
			}

			case ARRAY_LENGTH:
				addArg(code, insn.getArg(0));
				code.add(".length");
				break;

			case FILLED_NEW_ARRAY:
				filledNewArray((FilledNewArrayNode) insn, code);
				break;

			case FILL_ARRAY:
				FillArrayInsn arrayNode = (FillArrayInsn) insn;
				if (fallback) {
					String arrStr = arrayNode.dataToString();
					addArg(code, insn.getArg(0));
					code.add(" = {").add(arrStr.substring(1, arrStr.length() - 1)).add("} // fill-array");
				} else {
					fillArray(code, arrayNode);
				}
				break;

			case AGET:
				addArg(code, insn.getArg(0));
				code.add('[');
				addArg(code, insn.getArg(1), false);
				code.add(']');
				break;

			case APUT:
				addArg(code, insn.getArg(0));
				code.add('[');
				addArg(code, insn.getArg(1), false);
				code.add("] = ");
				addArg(code, insn.getArg(2), false);
				break;

			case IGET: {
				FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				instanceField(code, fieldInfo, insn.getArg(0));
				break;
			}
			case IPUT: {
				FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				instanceField(code, fieldInfo, insn.getArg(1));
				code.add(" = ");
				addArg(code, insn.getArg(0), false);
				break;
			}

			case SGET:
				staticField(code, (FieldInfo) ((IndexInsnNode) insn).getIndex());
				break;
			case SPUT:
				FieldInfo field = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				staticField(code, field);
				code.add(" = ");
				addArg(code, insn.getArg(0), false);
				break;

			case STR_CONCAT:
				boolean wrap = state.contains(Flags.BODY_ONLY);
				if (wrap) {
					code.add('(');
				}
				for (Iterator<InsnArg> it = insn.getArguments().iterator(); it.hasNext();) {
					addArg(code, it.next());
					if (it.hasNext()) {
						code.add(" + ");
					}
				}
				if (wrap) {
					code.add(')');
				}
				break;

			case MONITOR_ENTER:
				if (isFallback()) {
					code.add("monitor-enter(");
					addArg(code, insn.getArg(0));
					code.add(')');
				}
				break;

			case MONITOR_EXIT:
				if (isFallback()) {
					code.add("monitor-exit(");
					if (insn.getArgsCount() == 1) {
						addArg(code, insn.getArg(0));
					}
					code.add(')');
				}
				break;

			case TERNARY:
				makeTernary((TernaryInsn) insn, code, state);
				break;

			case ONE_ARG:
				addArg(code, insn.getArg(0));
				break;

			/* fallback mode instructions */
			case IF:
				fallbackOnlyInsn(insn);
				IfNode ifInsn = (IfNode) insn;
				code.add("if (");
				addArg(code, insn.getArg(0));
				code.add(' ');
				code.add(ifInsn.getOp().getSymbol()).add(' ');
				addArg(code, insn.getArg(1));
				code.add(") goto ").add(MethodGen.getLabelName(ifInsn.getTarget()));
				break;

			case GOTO:
				fallbackOnlyInsn(insn);
				code.add("goto ").add(MethodGen.getLabelName(((GotoNode) insn).getTarget()));
				break;

			case MOVE_EXCEPTION:
				fallbackOnlyInsn(insn);
				code.add("move-exception");
				break;

			case SWITCH:
				fallbackOnlyInsn(insn);
				SwitchInsn sw = (SwitchInsn) insn;
				code.add("switch(");
				addArg(code, insn.getArg(0));
				code.add(") {");
				code.incIndent();
				int[] keys = sw.getKeys();
				int[] targets = sw.getTargets();
				for (int i = 0; i < keys.length; i++) {
					code.startLine("case ").add(Integer.toString(keys[i])).add(": goto ");
					code.add(MethodGen.getLabelName(targets[i])).add(';');
				}
				code.startLine("default: goto ");
				code.add(MethodGen.getLabelName(sw.getDefaultCaseOffset())).add(';');
				code.decIndent();
				code.startLine('}');
				break;

			case NEW_INSTANCE:
				// only fallback - make new instance in constructor invoke
				fallbackOnlyInsn(insn);
				code.add("new ").add(insn.getResult().getInitType().toString());
				break;

			case PHI:
				fallbackOnlyInsn(insn);
				code.add(insn.getType().toString()).add('(');
				for (InsnArg insnArg : insn.getArguments()) {
					addArg(code, insnArg);
					code.add(' ');
				}
				code.add(')');
				break;

			case MOVE_RESULT:
				fallbackOnlyInsn(insn);
				code.add("move-result");
				break;

			case FILL_ARRAY_DATA:
				fallbackOnlyInsn(insn);
				code.add("fill-array " + insn.toString());
				break;

			case SWITCH_DATA:
				fallbackOnlyInsn(insn);
				code.add(insn.toString());
				break;

			default:
				throw new CodegenException(mth, "Unknown instruction: " + insn.getType());
		}
	}

	/**
	 * In most cases must be combined with new array instructions.
	 * Use one by one array fill (can be replaced with System.arrayCopy)
	 */
	private void fillArray(ICodeWriter code, FillArrayInsn arrayNode) throws CodegenException {
		code.add("// fill-array-data instruction");
		code.startLine();
		InsnArg arrArg = arrayNode.getArg(0);
		ArgType arrayType = arrArg.getType();
		ArgType elemType;
		if (arrayType.isTypeKnown() && arrayType.isArray()) {
			elemType = arrayType.getArrayElement();
		} else {
			ArgType elementType = arrayNode.getElementType(); // unknown type
			elemType = elementType.selectFirst();
		}
		List<LiteralArg> args = arrayNode.getLiteralArgs(elemType);
		int len = args.size();
		for (int i = 0; i < len; i++) {
			if (i != 0) {
				code.add(';');
				code.startLine();
			}
			addArg(code, arrArg);
			code.add('[').add(Integer.toString(i)).add("] = ").add(lit(args.get(i)));
		}
	}

	private void oneArgInsn(ICodeWriter code, InsnNode insn, Set<Flags> state, char op) throws CodegenException {
		boolean wrap = state.contains(Flags.BODY_ONLY);
		if (wrap) {
			code.add('(');
		}
		code.add(op);
		addArg(code, insn.getArg(0));
		if (wrap) {
			code.add(')');
		}
	}

	private void fallbackOnlyInsn(InsnNode insn) throws CodegenException {
		if (!fallback) {
			String msg = insn.getType() + " instruction can be used only in fallback mode";
			CodegenException e = new CodegenException(msg);
			mth.addError(msg, e);
			mth.getParentClass().getTopParentClass().add(AFlag.RESTART_CODEGEN);
			throw e;
		}
	}

	private void filledNewArray(FilledNewArrayNode insn, ICodeWriter code) throws CodegenException {
		if (!insn.contains(AFlag.DECLARE_VAR)) {
			code.add("new ");
			useType(code, insn.getArrayType());
		}
		code.add('{');
		int c = insn.getArgsCount();
		for (int i = 0; i < c; i++) {
			addArg(code, insn.getArg(i), false);
			if (i + 1 < c) {
				code.add(", ");
			}
		}
		code.add('}');
	}

	private void makeConstructor(ConstructorInsn insn, ICodeWriter code) throws CodegenException {
		ClassNode cls = mth.root().resolveClass(insn.getClassType());
		if (cls != null && cls.isAnonymous() && !fallback) {
			cls.ensureProcessed();
			inlineAnonymousConstructor(code, cls, insn);
			mth.getParentClass().addInlinedClass(cls);
			return;
		}
		if (insn.isSelf()) {
			throw new JadxRuntimeException("Constructor 'self' invoke must be removed!");
		}
		if (insn.isSuper()) {
			code.add("super");
		} else if (insn.isThis()) {
			code.add("this");
		} else {
			code.add("new ");
			useClass(code, insn.getClassType());
			GenericInfoAttr genericInfoAttr = insn.get(AType.GENERIC_INFO);
			if (genericInfoAttr != null) {
				code.add('<');
				if (genericInfoAttr.isExplicit()) {
					boolean first = true;
					for (ArgType type : genericInfoAttr.getGenericTypes()) {
						if (!first) {
							code.add(',');
						} else {
							first = false;
						}
						mgen.getClassGen().useType(code, type);
					}
				}
				code.add('>');
			}
		}
		MethodNode callMth = mth.root().resolveMethod(insn.getCallMth());
		generateMethodArguments(code, insn, 0, callMth);
	}

	private void inlineAnonymousConstructor(ICodeWriter code, ClassNode cls, ConstructorInsn insn) throws CodegenException {
		if (this.mth.getParentClass() == cls) {
			cls.remove(AFlag.ANONYMOUS_CLASS);
			cls.remove(AFlag.DONT_GENERATE);
			mth.getParentClass().getTopParentClass().add(AFlag.RESTART_CODEGEN);
			throw new CodegenException("Anonymous inner class unlimited recursion detected."
					+ " Convert class to inner: " + cls.getClassInfo().getFullName());
		}

		cls.add(AFlag.DONT_GENERATE);
		ArgType parent;
		if (cls.getInterfaces().size() == 1) {
			parent = cls.getInterfaces().get(0);
		} else {
			parent = cls.getSuperClass();
		}
		// hide empty anonymous constructors
		for (MethodNode ctor : cls.getMethods()) {
			if (ctor.contains(AFlag.ANONYMOUS_CONSTRUCTOR)
					&& RegionUtils.isEmpty(ctor.getRegion())) {
				ctor.add(AFlag.DONT_GENERATE);
			}
		}

		code.add("new ");
		if (parent == null) {
			code.add("Object");
		} else {
			useClass(code, parent);
		}
		MethodNode callMth = mth.root().resolveMethod(insn.getCallMth());
		generateMethodArguments(code, insn, 0, callMth);
		code.add(' ');

		ClassGen classGen = new ClassGen(cls, mgen.getClassGen().getParentGen());
		classGen.setOuterNameGen(mgen.getNameGen());
		classGen.addClassBody(code, true);
	}

	private void makeInvoke(InvokeNode insn, ICodeWriter code) throws CodegenException {
		InvokeType type = insn.getInvokeType();
		if (type == InvokeType.CUSTOM) {
			makeInvokeLambda(code, (InvokeCustomNode) insn);
			return;
		}
		MethodInfo callMth = insn.getCallMth();
		MethodNode callMthNode = mth.root().deepResolveMethod(callMth);

		int k = 0;
		switch (type) {
			case DIRECT:
			case VIRTUAL:
			case INTERFACE:
				InsnArg arg = insn.getArg(0);
				// FIXME: add 'this' for equals methods in scope
				if (!arg.isThis()) {
					addArgDot(code, arg);
				}
				k++;
				break;

			case SUPER:
				ClassInfo superCallCls = getClassForSuperCall(code, callMth);
				if (superCallCls != null) {
					useClass(code, superCallCls);
					code.add('.');
				}
				// use 'super' instead 'this' in 0 arg
				code.add("super").add('.');
				k++;
				break;

			case STATIC:
				ClassInfo insnCls = mth.getParentClass().getClassInfo();
				ClassInfo declClass = callMth.getDeclClass();
				if (!insnCls.equals(declClass)) {
					useClass(code, declClass);
					code.add('.');
				}
				break;
		}
		if (callMthNode != null) {
			code.attachAnnotation(callMthNode);
			code.add(callMthNode.getAlias());
		} else {
			code.add(callMth.getAlias());
		}
		generateMethodArguments(code, insn, k, callMthNode);
	}

	private void makeInvokeLambda(ICodeWriter code, InvokeCustomNode customNode) throws CodegenException {
		if (customNode.isUseRef()) {
			makeRefLambda(code, customNode);
			return;
		}
		if (fallback || !customNode.isInlineInsn()) {
			makeSimpleLambda(code, customNode);
			return;
		}
		MethodNode callMth = (MethodNode) customNode.getCallInsn().get(AType.METHOD_DETAILS);
		makeInlinedLambdaMethod(code, customNode, callMth);
	}

	private void makeRefLambda(ICodeWriter code, InvokeCustomNode customNode) {
		InsnNode callInsn = customNode.getCallInsn();
		if (callInsn instanceof ConstructorInsn) {
			MethodInfo callMth = ((ConstructorInsn) callInsn).getCallMth();
			useClass(code, callMth.getDeclClass());
			code.add("::new");
			return;
		}
		if (callInsn instanceof InvokeNode) {
			InvokeNode invokeInsn = (InvokeNode) callInsn;
			MethodInfo callMth = invokeInsn.getCallMth();
			if (customNode.getHandleType() == MethodHandleType.INVOKE_STATIC) {
				useClass(code, callMth.getDeclClass());
			} else {
				code.add("this");
			}
			code.add("::").add(callMth.getAlias());
		}
	}

	private void makeSimpleLambda(ICodeWriter code, InvokeCustomNode customNode) {
		try {
			InsnNode callInsn = customNode.getCallInsn();
			MethodInfo implMthInfo = customNode.getImplMthInfo();
			int implArgsCount = implMthInfo.getArgsCount();
			if (implArgsCount == 0) {
				code.add("()");
			} else {
				code.add('(');
				int callArgsCount = callInsn.getArgsCount();
				int startArg = callArgsCount - implArgsCount;
				if (customNode.getHandleType() != MethodHandleType.INVOKE_STATIC
						&& customNode.getArgsCount() > 0
						&& customNode.getArg(0).isThis()) {
					callInsn.getArg(0).add(AFlag.THIS);
				}
				if (startArg >= 0) {
					for (int i = startArg; i < callArgsCount; i++) {
						if (i != startArg) {
							code.add(", ");
						}
						addArg(code, callInsn.getArg(i));
					}
				} else {
					code.add("/* ERROR: " + startArg + " */");
				}
				code.add(')');
			}
			code.add(" -> {");
			if (fallback) {
				code.add(" // ").add(implMthInfo.toString());
			}
			code.incIndent();
			code.startLine();
			if (!implMthInfo.getReturnType().isVoid()) {
				code.add("return ");
			}
			makeInsn(callInsn, code, Flags.INLINE);
			code.add(";");

			code.decIndent();
			code.startLine('}');
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to generate 'invoke-custom' instruction: " + e.getMessage(), e);
		}
	}

	private void makeInlinedLambdaMethod(ICodeWriter code, InvokeCustomNode customNode, MethodNode callMth) throws CodegenException {
		MethodGen callMthGen = new MethodGen(mgen.getClassGen(), callMth);
		NameGen nameGen = callMthGen.getNameGen();
		nameGen.inheritUsedNames(this.mgen.getNameGen());

		List<ArgType> implArgs = customNode.getImplMthInfo().getArgumentsTypes();
		List<RegisterArg> callArgs = callMth.getArgRegs();
		if (implArgs.isEmpty()) {
			code.add("()");
		} else {
			int callArgsCount = callArgs.size();
			int startArg = callArgsCount - implArgs.size();
			for (int i = startArg; i < callArgsCount; i++) {
				if (i != startArg) {
					code.add(", ");
				}
				CodeVar argCodeVar = callArgs.get(i).getSVar().getCodeVar();
				code.add(nameGen.assignArg(argCodeVar));
			}
		}
		// force set external arg names into call method args
		int extArgsCount = customNode.getArgsCount();
		int startArg = customNode.getHandleType() == MethodHandleType.INVOKE_STATIC ? 0 : 1; // skip 'this' arg
		for (int i = startArg; i < extArgsCount; i++) {
			RegisterArg extArg = (RegisterArg) customNode.getArg(i);
			callArgs.get(i).setName(extArg.getName());
		}
		code.add(" -> {");
		code.incIndent();
		callMthGen.addInstructions(code);

		code.decIndent();
		code.startLine('}');
	}

	@Nullable
	private ClassInfo getClassForSuperCall(ICodeWriter code, MethodInfo callMth) {
		ClassNode useCls = mth.getParentClass();
		ClassInfo insnCls = useCls.getClassInfo();
		ClassInfo declClass = callMth.getDeclClass();
		if (insnCls.equals(declClass)) {
			return null;
		}
		ClassNode topClass = useCls.getTopParentClass();
		if (topClass.getClassInfo().equals(declClass)) {
			return declClass;
		}
		// search call class
		ClassNode nextParent = useCls;
		do {
			ClassInfo nextClsInfo = nextParent.getClassInfo();
			if (nextClsInfo.equals(declClass)
					|| ArgType.isInstanceOf(mth.root(), nextClsInfo.getType(), declClass.getType())) {
				if (nextParent == useCls) {
					return null;
				}
				return nextClsInfo;
			}
			nextParent = nextParent.getParentClass();
		} while (nextParent != null && nextParent != topClass);

		// search failed, just return parent class
		return useCls.getParentClass().getClassInfo();
	}

	void generateMethodArguments(ICodeWriter code, BaseInvokeNode insn, int startArgNum,
			@Nullable MethodNode mthNode) throws CodegenException {
		int k = startArgNum;
		if (mthNode != null && mthNode.contains(AFlag.SKIP_FIRST_ARG)) {
			k++;
		}
		int argsCount = insn.getArgsCount();
		code.add('(');
		boolean firstArg = true;
		if (k < argsCount) {
			for (int i = k; i < argsCount; i++) {
				InsnArg arg = insn.getArg(i);
				if (arg.contains(AFlag.SKIP_ARG)) {
					continue;
				}
				int argOrigPos = i - startArgNum;
				if (SkipMethodArgsAttr.isSkip(mthNode, argOrigPos)) {
					continue;
				}
				if (!firstArg) {
					code.add(", ");
				} else {
					firstArg = false;
				}
				if (i == argsCount - 1 && processVarArg(code, insn, arg)) {
					continue;
				}
				addArg(code, arg, false);
				firstArg = false;
			}
		}
		code.add(')');
	}

	/**
	 * Expand varArgs from filled array.
	 */
	private boolean processVarArg(ICodeWriter code, BaseInvokeNode invokeInsn, InsnArg lastArg) throws CodegenException {
		if (!invokeInsn.contains(AFlag.VARARG_CALL)) {
			return false;
		}
		if (!lastArg.getType().isArray() || !lastArg.isInsnWrap()) {
			return false;
		}
		InsnNode insn = ((InsnWrapArg) lastArg).getWrapInsn();
		if (insn.getType() != InsnType.FILLED_NEW_ARRAY) {
			return false;
		}
		int count = insn.getArgsCount();
		for (int i = 0; i < count; i++) {
			InsnArg elemArg = insn.getArg(i);
			addArg(code, elemArg, false);
			if (i < count - 1) {
				code.add(", ");
			}
		}
		return true;
	}

	private void makeTernary(TernaryInsn insn, ICodeWriter code, Set<Flags> state) throws CodegenException {
		boolean wrap = state.contains(Flags.BODY_ONLY);
		if (wrap) {
			code.add('(');
		}
		InsnArg first = insn.getArg(0);
		InsnArg second = insn.getArg(1);
		ConditionGen condGen = new ConditionGen(this);
		if (first.isTrue() && second.isFalse()) {
			condGen.add(code, insn.getCondition());
		} else {
			condGen.wrap(code, insn.getCondition());
			code.add(" ? ");
			addCastIfNeeded(code, first, second);
			addArg(code, first, false);
			code.add(" : ");
			addArg(code, second, false);
		}
		if (wrap) {
			code.add(')');
		}
	}

	private void addCastIfNeeded(ICodeWriter code, InsnArg first, InsnArg second) {
		if (first.isLiteral() && second.isLiteral()) {
			if (first.getType() == ArgType.BYTE) {
				long lit1 = ((LiteralArg) first).getLiteral();
				long lit2 = ((LiteralArg) second).getLiteral();
				if (lit1 != Byte.MAX_VALUE && lit1 != Byte.MIN_VALUE
						&& lit2 != Byte.MAX_VALUE && lit2 != Byte.MIN_VALUE) {
					code.add("(byte) ");
				}
			} else if (first.getType() == ArgType.SHORT) {
				long lit1 = ((LiteralArg) first).getLiteral();
				long lit2 = ((LiteralArg) second).getLiteral();
				if (lit1 != Short.MAX_VALUE && lit1 != Short.MIN_VALUE
						&& lit2 != Short.MAX_VALUE && lit2 != Short.MIN_VALUE) {
					code.add("(short) ");
				}
			} else if (first.getType() == ArgType.CHAR) {
				long lit1 = ((LiteralArg) first).getLiteral();
				long lit2 = ((LiteralArg) second).getLiteral();
				if (!NameMapper.isPrintableChar((char) (lit1))
						&& !NameMapper.isPrintableChar((char) (lit2))) {
					code.add("(char) ");
				}
			}
		}
	}

	private void makeArith(ArithNode insn, ICodeWriter code, Set<Flags> state) throws CodegenException {
		if (insn.contains(AFlag.ARITH_ONEARG)) {
			makeArithOneArg(insn, code);
			return;
		}
		// wrap insn in brackets for save correct operation order
		boolean wrap = state.contains(Flags.BODY_ONLY) && !insn.contains(AFlag.DONT_WRAP);
		if (wrap) {
			code.add('(');
		}
		addArg(code, insn.getArg(0));
		code.add(' ');
		code.add(insn.getOp().getSymbol());
		code.add(' ');
		addArg(code, insn.getArg(1));
		if (wrap) {
			code.add(')');
		}
	}

	private void makeArithOneArg(ArithNode insn, ICodeWriter code) throws CodegenException {
		ArithOp op = insn.getOp();
		InsnArg resArg = insn.getArg(0);
		InsnArg arg = insn.getArg(1);

		// "++" or "--"
		if (arg.isLiteral() && (op == ArithOp.ADD || op == ArithOp.SUB)) {
			LiteralArg lit = (LiteralArg) arg;
			if (lit.getLiteral() == 1 && lit.isInteger()) {
				addArg(code, resArg, false);
				String opSymbol = op.getSymbol();
				code.add(opSymbol).add(opSymbol);
				return;
			}
		}

		// +=, -=, ...
		addArg(code, resArg, false);
		code.add(' ').add(op.getSymbol()).add("= ");
		addArg(code, arg, false);
	}
}
