package jadx.core.codegen;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.FieldArg;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(InsnGen.class);

	protected final MethodGen mgen;
	protected final MethodNode mth;
	protected final RootNode root;
	protected final boolean fallback;

	private enum Flags {
		BODY_ONLY,
		BODY_ONLY_NOWRAP,
	}

	public InsnGen(MethodGen mgen, boolean fallback) {
		this.mgen = mgen;
		this.mth = mgen.getMethodNode();
		this.root = mth.dex().root();
		this.fallback = fallback;
	}

	private boolean isFallback() {
		return fallback;
	}

	public void addArgDot(CodeWriter code, InsnArg arg) throws CodegenException {
		int len = code.length();
		addArg(code, arg, true);
		if (len != code.length()) {
			code.add('.');
		}
	}

	public void addArg(CodeWriter code, InsnArg arg) throws CodegenException {
		addArg(code, arg, true);
	}

	public void addArg(CodeWriter code, InsnArg arg, boolean wrap) throws CodegenException {
		if (arg.isRegister()) {
			code.add(mgen.getNameGen().useArg((RegisterArg) arg));
		} else if (arg.isLiteral()) {
			code.add(lit((LiteralArg) arg));
		} else if (arg.isInsnWrap()) {
			Flags flag = wrap ? Flags.BODY_ONLY : Flags.BODY_ONLY_NOWRAP;
			makeInsn(((InsnWrapArg) arg).getWrapInsn(), code, flag);
		} else if (arg.isNamed()) {
			code.add(((NamedArg) arg).getName());
		} else if (arg.isField()) {
			FieldArg f = (FieldArg) arg;
			if (f.isStatic()) {
				staticField(code, f.getField());
			} else {
				instanceField(code, f.getField(), f.getInstanceArg());
			}
		} else {
			throw new CodegenException("Unknown arg type " + arg);
		}
	}

	public void assignVar(CodeWriter code, InsnNode insn) throws CodegenException {
		RegisterArg arg = insn.getResult();
		if (insn.contains(AFlag.DECLARE_VAR)) {
			declareVar(code, arg);
		} else {
			addArg(code, arg, false);
		}
	}

	public void declareVar(CodeWriter code, RegisterArg arg) {
		useType(code, arg.getType());
		code.add(' ');
		code.add(mgen.getNameGen().assignArg(arg));
	}

	private static String lit(LiteralArg arg) {
		return TypeGen.literalToString(arg.getLiteral(), arg.getType());
	}

	private void instanceField(CodeWriter code, FieldInfo field, InsnArg arg) throws CodegenException {
		FieldNode fieldNode = mth.getParentClass().searchField(field);
		if (fieldNode != null) {
			FieldReplaceAttr replace = fieldNode.get(AType.FIELD_REPLACE);
			if (replace != null) {
				FieldInfo info = replace.getFieldInfo();
				if (replace.isOuterClass()) {
					useClass(code, info.getDeclClass());
					code.add(".this");
				}
				return;
			}
		}
		addArgDot(code, arg);
		fieldNode = mth.dex().resolveField(field);
		if (fieldNode != null) {
			code.attachAnnotation(fieldNode);
		}
		code.add(field.getName());
	}

	public static void makeStaticFieldAccess(CodeWriter code, FieldInfo field, ClassGen clsGen) {
		ClassInfo declClass = field.getDeclClass();
		boolean fieldFromThisClass = clsGen.getClassNode().getFullName().startsWith(declClass.getFullName());
		if (!fieldFromThisClass) {
			// Android specific resources class handler
			ClassInfo parentClass = declClass.getParentClass();
			if (parentClass != null && parentClass.getShortName().equals("R")) {
				clsGen.useClass(code, parentClass);
				code.add('.');
				code.add(declClass.getShortName());
			} else {
				clsGen.useClass(code, declClass);
			}
			code.add('.');
		}
		FieldNode fieldNode = clsGen.getClassNode().dex().resolveField(field);
		if (fieldNode != null) {
			code.attachAnnotation(fieldNode);
		}
		code.add(field.getName());
	}

	protected void staticField(CodeWriter code, FieldInfo field) {
		makeStaticFieldAccess(code, field, mgen.getClassGen());
	}

	public void useClass(CodeWriter code, ClassInfo cls) {
		mgen.getClassGen().useClass(code, cls);
	}

	private void useType(CodeWriter code, ArgType type) {
		mgen.getClassGen().useType(code, type);
	}

	public boolean makeInsn(InsnNode insn, CodeWriter code) throws CodegenException {
		return makeInsn(insn, code, null);
	}

	private boolean makeInsn(InsnNode insn, CodeWriter code, Flags flag) throws CodegenException {
		try {
			if (insn.getType() == InsnType.NOP) {
				return false;
			}
			EnumSet<Flags> state = EnumSet.noneOf(Flags.class);
			if (flag == Flags.BODY_ONLY || flag == Flags.BODY_ONLY_NOWRAP) {
				state.add(flag);
				makeInsnBody(code, insn, state);
			} else {
				code.startLine();
				if (insn.getSourceLine() != 0) {
					code.attachSourceLine(insn.getSourceLine());
				}
				if (insn.getResult() != null && insn.getType() != InsnType.ARITH_ONEARG) {
					assignVar(code, insn);
					code.add(" = ");
				}
				makeInsnBody(code, insn, state);
				code.add(';');
			}
		} catch (Throwable th) {
			throw new CodegenException(mth, "Error generate insn: " + insn, th);
		}
		return true;
	}

	private void makeInsnBody(CodeWriter code, InsnNode insn, EnumSet<Flags> state) throws CodegenException {
		switch (insn.getType()) {
			case CONST_STR:
				String str = ((ConstStringNode) insn).getString();
				code.add(StringUtils.unescapeString(str));
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

			case ARITH_ONEARG:
				makeArithOneArg((ArithNode) insn, code);
				break;

			case NEG: {
				boolean wrap = state.contains(Flags.BODY_ONLY);
				if (wrap) {
					code.add('(');
				}
				code.add('-');
				addArg(code, insn.getArg(0));
				if (wrap) {
					code.add(')');
				}
				break;
			}

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
				code.add("? 0 : -1))");
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
				ArgType arrayType = insn.getResult().getType();
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

			case FILL_ARRAY:
				fillArray((FillArrayNode) insn, code);
				break;

			case FILLED_NEW_ARRAY:
				filledNewArray(insn, code);
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
				for (Iterator<InsnArg> it = insn.getArguments().iterator(); it.hasNext(); ) {
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
					addArg(code, insn.getArg(0));
					code.add(')');
				}
				break;

			case MOVE_EXCEPTION:
				if (isFallback()) {
					code.add("move-exception");
				} else {
					addArg(code, insn.getArg(0));
				}
				break;

			case TERNARY:
				makeTernary((TernaryInsn) insn, code, state);
				break;

			case ARGS:
				addArg(code, insn.getArg(0));
				break;

			case PHI:
				break;

			/* fallback mode instructions */
			case IF:
				assert isFallback() : "if insn in not fallback mode";
				IfNode ifInsn = (IfNode) insn;
				code.add("if (");
				addArg(code, insn.getArg(0));
				code.add(' ');
				code.add(ifInsn.getOp().getSymbol()).add(' ');
				addArg(code, insn.getArg(1));
				code.add(") goto ").add(MethodGen.getLabelName(ifInsn.getTarget()));
				break;

			case GOTO:
				assert isFallback();
				code.add("goto ").add(MethodGen.getLabelName(((GotoNode) insn).getTarget()));
				break;

			case SWITCH:
				assert isFallback();
				SwitchNode sw = (SwitchNode) insn;
				code.add("switch(");
				addArg(code, insn.getArg(0));
				code.add(") {");
				code.incIndent();
				for (int i = 0; i < sw.getCasesCount(); i++) {
					String key = sw.getKeys()[i].toString();
					code.startLine("case ").add(key).add(": goto ");
					code.add(MethodGen.getLabelName(sw.getTargets()[i])).add(';');
				}
				code.startLine("default: goto ");
				code.add(MethodGen.getLabelName(sw.getDefaultCaseOffset())).add(';');
				code.decIndent();
				code.startLine('}');
				break;

			case NEW_INSTANCE:
				// only fallback - make new instance in constructor invoke
				assert isFallback();
				code.add("new " + insn.getResult().getType());
				break;

			default:
				throw new CodegenException(mth, "Unknown instruction: " + insn.getType());
		}
	}

	private void filledNewArray(InsnNode insn, CodeWriter code) throws CodegenException {
		int c = insn.getArgsCount();
		code.add("new ");
		useType(code, insn.getResult().getType());
		code.add('{');
		for (int i = 0; i < c; i++) {
			addArg(code, insn.getArg(i));
			if (i + 1 < c) {
				code.add(", ");
			}
		}
		code.add('}');
	}

	private void fillArray(FillArrayNode insn, CodeWriter code) throws CodegenException {
		ArgType insnArrayType = insn.getResult().getType();
		ArgType insnElementType = insnArrayType.getArrayElement();
		ArgType elType = insn.getElementType();
		if (!elType.equals(insnElementType) && !insnArrayType.equals(ArgType.OBJECT)) {
			ErrorsCounter.methodError(mth,
					"Incorrect type for fill-array insn " + InsnUtils.formatOffset(insn.getOffset())
							+ ", element type: " + elType + ", insn element type: " + insnElementType
			);
			if (!elType.isTypeKnown()) {
				elType = insnElementType.isTypeKnown() ? insnElementType : elType.selectFirst();
			}
		}
		StringBuilder str = new StringBuilder();
		Object data = insn.getData();
		switch (elType.getPrimitiveType()) {
			case BOOLEAN:
			case BYTE:
				byte[] array = (byte[]) data;
				for (byte b : array) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case SHORT:
			case CHAR:
				short[] sarray = (short[]) data;
				for (short b : sarray) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case INT:
			case FLOAT:
				int[] iarray = (int[]) data;
				for (int b : iarray) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case LONG:
			case DOUBLE:
				long[] larray = (long[]) data;
				for (long b : larray) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;

			default:
				throw new CodegenException(mth, "Unknown type: " + elType);
		}
		int len = str.length();
		str.delete(len - 2, len);
		code.add("new ");
		useType(code, elType);
		code.add("[]{").add(str.toString()).add('}');
	}

	private void makeConstructor(ConstructorInsn insn, CodeWriter code)
			throws CodegenException {
		ClassNode cls = mth.dex().resolveClass(insn.getClassType());
		if (cls != null && cls.isAnonymous() && !fallback) {
			// anonymous class construction
			ClassInfo parent;
			if (cls.getInterfaces().size() == 1) {
				parent = cls.getInterfaces().get(0);
			} else {
				parent = cls.getSuperClass();
			}
			cls.add(AFlag.DONT_GENERATE);
			MethodNode defCtr = cls.getDefaultConstructor();
			if (defCtr != null) {
				if (RegionUtils.notEmpty(defCtr.getRegion())) {
					defCtr.add(AFlag.ANONYMOUS_CONSTRUCTOR);
				} else {
					defCtr.add(AFlag.DONT_GENERATE);
				}
			}
			code.add("new ");
			if (parent == null) {
				code.add("Object");
			} else {
				useClass(code, parent);
			}
			code.add("() ");
			new ClassGen(cls, mgen.getClassGen().getParentGen(), fallback).addClassBody(code);
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
		}
		generateArguments(code, insn, 0, mth.dex().resolveMethod(insn.getCallMth()));
	}

	private void makeInvoke(InvokeNode insn, CodeWriter code) throws CodegenException {
		MethodInfo callMth = insn.getCallMth();

		// inline method
		MethodNode callMthNode = mth.dex().resolveMethod(callMth);
		if (callMthNode != null && inlineMethod(callMthNode, insn, code)) {
			return;
		}

		int k = 0;
		InvokeType type = insn.getInvokeType();
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
		}
		code.add(callMth.getName());
		generateArguments(code, insn, k, callMthNode);
	}

	private void generateArguments(CodeWriter code, InsnNode insn, int k, MethodNode callMth) throws CodegenException {
		if (callMth != null && callMth.contains(AFlag.SKIP_FIRST_ARG)) {
			k++;
		}
		int argsCount = insn.getArgsCount();
		if (callMth != null && callMth.isArgsOverload()) {
			// add additional argument casts for overloaded methods
			List<ArgType> originalType = callMth.getMethodInfo().getArgumentsTypes();
			int origPos = 0;
			code.add('(');
			for (int i = k; i < argsCount; i++) {
				InsnArg arg = insn.getArg(i);
				ArgType origType = originalType.get(origPos);
				if (!arg.getType().equals(origType)) {
					code.add('(');
					useType(code, origType);
					code.add(')');
					addArg(code, arg, true);
				} else {
					addArg(code, arg, false);
				}
				if (i < argsCount - 1) {
					code.add(", ");
				}
				origPos++;
			}
			code.add(')');
		} else {
			code.add('(');
			if (k < argsCount) {
				addArg(code, insn.getArg(k), false);
				for (int i = k + 1; i < argsCount; i++) {
					code.add(", ");
					addArg(code, insn.getArg(i), false);
				}
			}
			code.add(')');
		}
	}

	private boolean inlineMethod(MethodNode callMthNode, InvokeNode insn, CodeWriter code) throws CodegenException {
		MethodInlineAttr mia = callMthNode.get(AType.METHOD_INLINE);
		if (mia == null) {
			return false;
		}
		InsnNode inl = mia.getInsn();
		if (callMthNode.getMethodInfo().getArgumentsTypes().isEmpty()) {
			makeInsn(inl, code, Flags.BODY_ONLY);
		} else {
			// remap args
			InsnArg[] regs = new InsnArg[callMthNode.getRegsCount()];
			List<RegisterArg> callArgs = callMthNode.getArguments(true);
			for (int i = 0; i < callArgs.size(); i++) {
				InsnArg arg = insn.getArg(i);
				RegisterArg callArg = callArgs.get(i);
				regs[callArg.getRegNum()] = arg;
			}
			// replace args
			List<RegisterArg> inlArgs = new ArrayList<RegisterArg>();
			inl.getRegisterArgs(inlArgs);
			Map<RegisterArg, InsnArg> toRevert = new HashMap<RegisterArg, InsnArg>();
			for (RegisterArg r : inlArgs) {
				int regNum = r.getRegNum();
				if (regNum >= regs.length) {
					LOG.warn("Unknown register number {} in method call: {} from {}", r, callMthNode, mth);
				} else {
					InsnArg repl = regs[regNum];
					if (repl == null) {
						LOG.warn("Not passed register {} in method call: {} from {}", r, callMthNode, mth);
					} else {
						inl.replaceArg(r, repl);
						toRevert.put(r, repl);
					}
				}
			}
			makeInsn(inl, code, Flags.BODY_ONLY);
			// revert changes in 'MethodInlineAttr'
			for (Map.Entry<RegisterArg, InsnArg> e : toRevert.entrySet()) {
				inl.replaceArg(e.getValue(), e.getKey());
			}
		}
		return true;
	}

	private void makeTernary(TernaryInsn insn, CodeWriter code, EnumSet<Flags> state) throws CodegenException {
		boolean wrap = state.contains(Flags.BODY_ONLY);
		if (wrap) {
			code.add('(');
		}
		InsnArg first = insn.getArg(0);
		InsnArg second = insn.getArg(1);
		ConditionGen condGen = new ConditionGen(this);
		if (first.equals(LiteralArg.TRUE) && second.equals(LiteralArg.FALSE)) {
			condGen.add(code, insn.getCondition());
		} else {
			condGen.wrap(code, insn.getCondition());
			code.add(" ? ");
			addArg(code, first, false);
			code.add(" : ");
			addArg(code, second, false);
		}
		if (wrap) {
			code.add(')');
		}
	}

	private void makeArith(ArithNode insn, CodeWriter code, EnumSet<Flags> state) throws CodegenException {
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

	private void makeArithOneArg(ArithNode insn, CodeWriter code) throws CodegenException {
		ArithOp op = insn.getOp();
		InsnArg arg = insn.getArg(0);
		// "++" or "--"
		if (arg.isLiteral() && (op == ArithOp.ADD || op == ArithOp.SUB)) {
			LiteralArg lit = (LiteralArg) arg;
			if (lit.isInteger() && lit.getLiteral() == 1) {
				assignVar(code, insn);
				String opSymbol = op.getSymbol();
				code.add(opSymbol).add(opSymbol);
				return;
			}
		}
		// +=, -= ...
		assignVar(code, insn);
		code.add(' ').add(op.getSymbol()).add("= ");
		addArg(code, arg, false);
	}
}
