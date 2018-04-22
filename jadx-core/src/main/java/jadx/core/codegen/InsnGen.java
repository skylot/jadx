package jadx.core.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.FieldArg;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.Named;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

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
		this.root = mth.dex().root();
		this.fallback = fallback;
	}

	private boolean isFallback() {
		return fallback;
	}

	public void addArgDot(CodeWriter code, InsnArg arg) throws CodegenException {
		int len = code.bufLength();
		addArg(code, arg, true);
		if (len != code.bufLength()) {
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
			code.add(((Named) arg).getName());
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
		if (arg.getSVar().contains(AFlag.FINAL)) {
			code.add("final ");
		}
		useType(code, arg.getType());
		code.add(' ');
		code.add(mgen.getNameGen().assignArg(arg));
	}

	private String lit(LiteralArg arg) {
		return TypeGen.literalToString(arg.getLiteral(), arg.getType(), mth);
	}

	private void instanceField(CodeWriter code, FieldInfo field, InsnArg arg) throws CodegenException {
		ClassNode pCls = mth.getParentClass();
		FieldNode fieldNode = pCls.searchField(field);
		while (fieldNode == null
				&& pCls.getParentClass() != pCls
				&& pCls.getParentClass() != null) {
			pCls = pCls.getParentClass();
			fieldNode = pCls.searchField(field);
		}
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
		code.add(field.getAlias());
	}

	public static void makeStaticFieldAccess(CodeWriter code, FieldInfo field, ClassGen clsGen) {
		ClassInfo declClass = field.getDeclClass();
		boolean fieldFromThisClass = clsGen.getClassNode().getClassInfo().equals(declClass);
		if (!fieldFromThisClass) {
			// Android specific resources class handler
			if (!handleAppResField(code, clsGen, declClass)) {
				clsGen.useClass(code, declClass);
			}
			code.add('.');
		}
		FieldNode fieldNode = clsGen.getClassNode().dex().resolveField(field);
		if (fieldNode != null) {
			code.attachAnnotation(fieldNode);
		}
		code.add(field.getAlias());
	}

	protected void staticField(CodeWriter code, FieldInfo field) {
		makeStaticFieldAccess(code, field, mgen.getClassGen());
	}

	public void useClass(CodeWriter code, ArgType type) {
		mgen.getClassGen().useClass(code, type);
	}

	public void useClass(CodeWriter code, ClassInfo cls) {
		mgen.getClassGen().useClass(code, cls);
	}

	protected void useType(CodeWriter code, ArgType type) {
		mgen.getClassGen().useType(code, type);
	}

	public boolean makeInsn(InsnNode insn, CodeWriter code) throws CodegenException {
		return makeInsn(insn, code, null);
	}

	protected boolean makeInsn(InsnNode insn, CodeWriter code, Flags flag) throws CodegenException {
		try {
			Set<Flags> state = EnumSet.noneOf(Flags.class);
			if (flag == Flags.BODY_ONLY || flag == Flags.BODY_ONLY_NOWRAP) {
				state.add(flag);
				makeInsnBody(code, insn, state);
			} else {
				if (flag != Flags.INLINE) {
					code.startLineWithNum(insn.getSourceLine());
				}
				if (insn.getResult() != null && !insn.contains(AFlag.ARITH_ONEARG)) {
					assignVar(code, insn);
					code.add(" = ");
				}
				makeInsnBody(code, insn, state);
				if (flag != Flags.INLINE) {
					code.add(';');
				}
			}
		} catch (Throwable th) {
			throw new CodegenException(mth, "Error generate insn: " + insn, th);
		}
		return true;
	}

	private void makeInsnBody(CodeWriter code, InsnNode insn, Set<Flags> state) throws CodegenException {
		switch (insn.getType()) {
			case CONST_STR:
				String str = ((ConstStringNode) insn).getString();
				code.add(mth.dex().root().getStringUtils().unescapeString(str));
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
				oneArgInsn(code, insn, state, '~');
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

			case FILL_ARRAY:
				fallbackOnlyInsn(insn);
				FillArrayNode arrayNode = (FillArrayNode) insn;
				Object data = arrayNode.getData();
				String arrStr;
				if (data instanceof int[]) {
					arrStr = Arrays.toString((int[]) data);
				} else if (data instanceof short[]) {
					arrStr = Arrays.toString((short[]) data);
				} else if (data instanceof byte[]) {
					arrStr = Arrays.toString((byte[]) data);
				} else if (data instanceof long[]) {
					arrStr = Arrays.toString((long[]) data);
				} else {
					arrStr = "?";
				}
				code.add('{').add(arrStr.substring(1, arrStr.length() - 1)).add('}');
				break;

			case NEW_INSTANCE:
				// only fallback - make new instance in constructor invoke
				fallbackOnlyInsn(insn);
				code.add("new ").add(insn.getResult().getType().toString());
				break;

			case PHI:
			case MERGE:
				fallbackOnlyInsn(insn);
				code.add(insn.getType().toString()).add("(");
				for (InsnArg insnArg : insn.getArguments()) {
					addArg(code, insnArg);
					code.add(' ');
				}
				code.add(")");
				break;

			default:
				throw new CodegenException(mth, "Unknown instruction: " + insn.getType());
		}
	}

	private void oneArgInsn(CodeWriter code, InsnNode insn, Set<Flags> state, char op) throws CodegenException {
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
			throw new CodegenException(insn.getType() + " can be used only in fallback mode");
		}
	}

	private void filledNewArray(FilledNewArrayNode insn, CodeWriter code) throws CodegenException {
		code.add("new ");
		useType(code, insn.getArrayType());
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

	private void makeConstructor(ConstructorInsn insn, CodeWriter code)
			throws CodegenException {
		ClassNode cls = mth.dex().resolveClass(insn.getClassType());
		if (cls != null && cls.contains(AFlag.ANONYMOUS_CLASS) && !fallback) {
			inlineAnonymousConstr(code, cls, insn);
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
		MethodNode callMth = mth.dex().resolveMethod(insn.getCallMth());
		generateMethodArguments(code, insn, 0, callMth);
	}

	private void inlineAnonymousConstr(CodeWriter code, ClassNode cls, ConstructorInsn insn) throws CodegenException {
		// anonymous class construction
		if (cls.contains(AFlag.DONT_GENERATE)) {
			code.add("/* anonymous class already generated */");
			ErrorsCounter.methodError(mth, "Anonymous class already generated: " + cls);
			return;
		}
		ArgType parent;
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
		MethodNode callMth = mth.dex().resolveMethod(insn.getCallMth());
		generateMethodArguments(code, insn, 0, callMth);
		code.add(' ');
		new ClassGen(cls, mgen.getClassGen().getParentGen()).addClassBody(code);
	}

	private void makeInvoke(InvokeNode insn, CodeWriter code) throws CodegenException {
		MethodInfo callMth = insn.getCallMth();

		// inline method
		MethodNode callMthNode = mth.root().deepResolveMethod(callMth);
		if (callMthNode != null) {
			if (inlineMethod(callMthNode, insn, code)) {
				return;
			}
			callMth = callMthNode.getMethodInfo();
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
				ClassInfo insnCls = mth.getParentClass().getAlias();
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
		code.add(callMth.getAlias());
		generateMethodArguments(code, insn, k, callMthNode);
	}

	void generateMethodArguments(CodeWriter code, InsnNode insn, int startArgNum,
			@Nullable MethodNode callMth) throws CodegenException {
		int k = startArgNum;
		if (callMth != null && callMth.contains(AFlag.SKIP_FIRST_ARG)) {
			k++;
		}
		int argsCount = insn.getArgsCount();
		code.add('(');
		boolean firstArg = true;
		if (k < argsCount) {
			boolean overloaded = callMth != null && callMth.isArgsOverload();
			for (int i = k; i < argsCount; i++) {
				InsnArg arg = insn.getArg(i);
				if (arg.contains(AFlag.SKIP_ARG)) {
					continue;
				}
				RegisterArg callArg = getCallMthArg(callMth, i - startArgNum);
				if (callArg != null && callArg.contains(AFlag.SKIP_ARG)) {
					continue;
				}
				if (!firstArg) {
					code.add(", ");
				}
				boolean cast = overloaded && processOverloadedArg(code, callMth, arg, i - startArgNum);
				if (!cast && i == argsCount - 1 && processVarArg(code, callMth, arg)) {
					continue;
				}
				addArg(code, arg, false);
				firstArg = false;
			}
		}
		code.add(')');
	}

	private static RegisterArg getCallMthArg(@Nullable MethodNode callMth, int num) {
		if (callMth == null) {
			return null;
		}
		List<RegisterArg> args = callMth.getArguments(false);
		if (args != null && num < args.size()) {
			return args.get(num);
		}
		return null;
	}

	/**
	 * Add additional cast for overloaded method argument.
	 */
	private boolean processOverloadedArg(CodeWriter code, MethodNode callMth, InsnArg arg, int origPos) {
		ArgType origType = callMth.getMethodInfo().getArgumentsTypes().get(origPos);
		if (!arg.getType().equals(origType)) {
			code.add('(');
			useType(code, origType);
			code.add(") ");
			return true;
		}
		return false;
	}

	/**
	 * Expand varArgs from filled array.
	 */
	private boolean processVarArg(CodeWriter code, MethodNode callMth, InsnArg lastArg) throws CodegenException {
		if (callMth == null || !callMth.getAccessFlags().isVarArgs()) {
			return false;
		}
		if (!lastArg.getType().isArray() || !lastArg.isInsnWrap()) {
			return false;
		}
		InsnNode insn = ((InsnWrapArg) lastArg).getWrapInsn();
		if (insn.getType() == InsnType.FILLED_NEW_ARRAY) {
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
		return false;
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
			InsnNode inlCopy = inl.copy();
			List<RegisterArg> inlArgs = new ArrayList<>();
			inlCopy.getRegisterArgs(inlArgs);
			for (RegisterArg r : inlArgs) {
				int regNum = r.getRegNum();
				if (regNum >= regs.length) {
					LOG.warn("Unknown register number {} in method call: {} from {}", r, callMthNode, mth);
				} else {
					InsnArg repl = regs[regNum];
					if (repl == null) {
						LOG.warn("Not passed register {} in method call: {} from {}", r, callMthNode, mth);
					} else {
						inlCopy.replaceArg(r, repl);
					}
				}
			}
			makeInsn(inlCopy, code, Flags.BODY_ONLY);
		}
		return true;
	}

	private void makeTernary(TernaryInsn insn, CodeWriter code, Set<Flags> state) throws CodegenException {
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

	private void makeArith(ArithNode insn, CodeWriter code, Set<Flags> state) throws CodegenException {
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

	private void makeArithOneArg(ArithNode insn, CodeWriter code) throws CodegenException {
		ArithOp op = insn.getOp();
		InsnArg arg = insn.getArg(1);
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
