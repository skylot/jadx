package jadx.core.codegen;

import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.attributes.MethodInlineAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.ConstClassInsn;
import jadx.core.dex.instructions.ConstStringInsn;
import jadx.core.dex.instructions.FillArrayOp;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
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
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.CodegenException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(InsnGen.class);

	protected final MethodGen mgen;
	protected final MethodNode mth;
	protected final RootNode root;
	private final boolean fallback;

	private static enum IGState {
		SKIP,

		NO_SEMICOLON,
		NO_RESULT,

		BODY_ONLY,
		BODY_ONLY_NOWRAP,
	}

	public InsnGen(MethodGen mgen, MethodNode mth, boolean fallback) {
		this.mgen = mgen;
		this.mth = mth;
		this.root = mth.dex().root();
		this.fallback = fallback;
	}

	private boolean isFallback() {
		return fallback;
	}

	public String arg(InsnNode insn, int arg) throws CodegenException {
		return arg(insn.getArg(arg));
	}

	public String arg(InsnArg arg) throws CodegenException {
		return arg(arg, true);
	}

	public String arg(InsnArg arg, boolean wrap) throws CodegenException {
		if (arg.isRegister()) {
			return mgen.makeArgName((RegisterArg) arg);
		} else if (arg.isLiteral()) {
			return lit((LiteralArg) arg);
		} else if (arg.isInsnWrap()) {
			CodeWriter code = new CodeWriter();
			IGState flag = wrap ? IGState.BODY_ONLY : IGState.BODY_ONLY_NOWRAP;
			makeInsn(((InsnWrapArg) arg).getWrapInsn(), code, flag);
			return code.toString();
		} else if (arg.isNamed()) {
			return ((NamedArg) arg).getName();
		} else if (arg.isField()) {
			FieldArg f = (FieldArg) arg;
			if (f.isStatic()) {
				return sfield(f.getField());
			} else {
				RegisterArg regArg = new RegisterArg(f.getRegNum());
				regArg.setTypedVar(f.getTypedVar());
				return ifield(f.getField(), regArg);
			}
		} else {
			throw new CodegenException("Unknown arg type " + arg);
		}
	}

	public String assignVar(InsnNode insn) throws CodegenException {
		RegisterArg arg = insn.getResult();
		if (insn.getAttributes().contains(AttributeType.DECLARE_VARIABLE)) {
			return declareVar(arg);
		} else {
			return arg(arg);
		}
	}

	public String declareVar(RegisterArg arg) {
		return useType(arg.getType()) + " " + mgen.assignArg(arg);
	}

	private String lit(LiteralArg arg) {
		return TypeGen.literalToString(arg.getLiteral(), arg.getType());
	}

	private String ifield(FieldInfo field, InsnArg arg) throws CodegenException {
		return arg(arg) + "." + field.getName();
	}

	private String sfield(FieldInfo field) {
		String thisClass = mth.getParentClass().getFullName();
		if (field.getDeclClass().getFullName().equals(thisClass)) {
			return field.getName();
		} else {
			return useClass(field.getDeclClass()) + '.' + field.getName();
		}
	}

	private void fieldPut(IndexInsnNode insn) {
		FieldInfo field = (FieldInfo) insn.getIndex();
		String thisClass = mth.getParentClass().getFullName();
		if (field.getDeclClass().getFullName().equals(thisClass)) {
			// if we generate this field - don't init if its final and used
			FieldNode fn = mth.getParentClass().searchField(field);
			if (fn != null && fn.getAccessFlags().isFinal())
				fn.getAttributes().remove(AttributeType.FIELD_VALUE);
		}
	}

	public String useClass(ClassInfo cls) {
		return mgen.getClassGen().useClass(cls);
	}

	private String useType(ArgType type) {
		return TypeGen.translate(mgen.getClassGen(), type);
	}

	public boolean makeInsn(InsnNode insn, CodeWriter code) throws CodegenException {
		return makeInsn(insn, code, null);
	}

	private boolean makeInsn(InsnNode insn, CodeWriter code, IGState flag) throws CodegenException {
		try {
			EnumSet<IGState> state = EnumSet.noneOf(IGState.class);
			if (flag == IGState.BODY_ONLY || flag == IGState.BODY_ONLY_NOWRAP) {
				state.add(flag);
				makeInsnBody(code, insn, state);
			} else {
				CodeWriter body = new CodeWriter(code.getIndent());
				makeInsnBody(body, insn, state);
				if (state.contains(IGState.SKIP)) {
					return false;
				}

				code.startLine();
				if (insn.getSourceLine() != 0) {
					code.attachAnnotation(insn.getSourceLine());
				}
				if (insn.getResult() != null && !state.contains(IGState.NO_RESULT)) {
					code.add(assignVar(insn)).add(" = ");
				}
				code.add(body);

				if (!state.contains(IGState.NO_SEMICOLON)) {
					code.add(';');
				}
			}
		} catch (Throwable th) {
			throw new CodegenException(mth, "Error generate insn: " + insn, th);
		}
		return true;
	}

	private void makeInsnBody(CodeWriter code, InsnNode insn, EnumSet<IGState> state) throws CodegenException {
		switch (insn.getType()) {
			case CONST_STR:
				String str = ((ConstStringInsn) insn).getString();
				code.add(StringUtils.unescapeString(str));
				break;

			case CONST_CLASS:
				ArgType clsType = ((ConstClassInsn) insn).getClsType();
				code.add(useType(clsType)).add(".class");
				break;

			case CONST:
				LiteralArg arg = (LiteralArg) insn.getArg(0);
				code.add(lit(arg));
				break;

			case MOVE:
				code.add(arg(insn.getArg(0), false));
				break;

			case CHECK_CAST:
			case CAST:
				boolean wrap = state.contains(IGState.BODY_ONLY);
				if (wrap)
					code.add("(");
				code.add("(");
				code.add(useType(((ArgType) ((IndexInsnNode) insn).getIndex())));
				code.add(") ");
				code.add(arg(insn.getArg(0)));
				if (wrap)
					code.add(")");
				break;

			case ARITH:
				makeArith((ArithNode) insn, code, state);
				break;

			case NEG:
				String base = "-" + arg(insn.getArg(0));
				if (state.contains(IGState.BODY_ONLY)) {
					code.add('(').add(base).add(')');
				} else {
					code.add(base);
				}
				break;

			case RETURN:
				if (insn.getArgsCount() != 0)
					code.add("return ").add(arg(insn.getArg(0), false));
				else
					code.add("return");
				break;

			case BREAK:
				code.add("break");
				break;

			case CONTINUE:
				code.add("continue");
				break;

			case THROW:
				code.add("throw ").add(arg(insn.getArg(0), true));
				break;

			case CMP_L:
			case CMP_G:
				code.add(String.format("(%1$s > %2$s ? 1 : (%1$s == %2$s ? 0 : -1))", arg(insn, 0), arg(insn, 1)));
				break;

			case INSTANCE_OF:
				code.add('(').add(arg(insn, 0)).add(" instanceof ")
						.add(useType((ArgType) ((IndexInsnNode) insn).getIndex())).add(')');
				break;

			case CONSTRUCTOR:
				makeConstructor((ConstructorInsn) insn, code, state);
				break;

			case INVOKE:
				makeInvoke((InvokeNode) insn, code);
				break;

			case NEW_ARRAY: {
				ArgType arrayType = insn.getResult().getType();
				int dim = arrayType.getArrayDimension();
				code.add("new ").add(useType(arrayType.getArrayRootElement())).add('[').add(arg(insn, 0)).add(']');
				for (int i = 0; i < dim - 1; i++)
					code.add("[]");
				break;
			}

			case ARRAY_LENGTH:
				code.add(arg(insn, 0)).add(".length");
				break;

			case FILL_ARRAY:
				fillArray((FillArrayOp) insn, code);
				break;

			case FILLED_NEW_ARRAY:
				filledNewArray(insn, code);
				break;

			case AGET:
				code.add(arg(insn.getArg(0))).add('[').add(arg(insn.getArg(1), false)).add(']');
				break;

			case APUT:
				code.add(arg(insn, 0)).add('[').add(arg(insn, 1)).add("] = ").add(arg(insn, 2));
				break;

			case IGET: {
				FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				code.add(ifield(fieldInfo, insn.getArg(0)));
				break;
			}
			case IPUT: {
				FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				code.add(ifield(fieldInfo, insn.getArg(1))).add(" = ").add(arg(insn.getArg(0), false));
				break;
			}

			case SGET:
				code.add(sfield((FieldInfo) ((IndexInsnNode) insn).getIndex()));
				break;
			case SPUT:
				IndexInsnNode node = (IndexInsnNode) insn;
				fieldPut(node);
				code.add(sfield((FieldInfo) node.getIndex())).add(" = ").add(arg(node.getArg(0), false));
				break;

			case STR_CONCAT:
				StringBuilder sb = new StringBuilder();
				for (Iterator<InsnArg> it = insn.getArguments().iterator(); it.hasNext(); ) {
					sb.append(arg(it.next()));
					if (it.hasNext()) {
						sb.append(" + ");
					}
				}
				// TODO: wrap in braces only if necessary
				if (state.contains(IGState.BODY_ONLY)) {
					code.add('(').add(sb.toString()).add(')');
				} else {
					code.add(sb.toString());
				}
				break;

			case MONITOR_ENTER:
				if (isFallback()) {
					code.add("monitor-enter(").add(arg(insn.getArg(0))).add(')');
				} else {
					state.add(IGState.SKIP);
				}
				break;

			case MONITOR_EXIT:
				if (isFallback()) {
					code.add("monitor-exit(").add(arg(insn, 0)).add(')');
				} else {
					state.add(IGState.SKIP);
				}
				break;

			case MOVE_EXCEPTION:
				if (isFallback()) {
					code.add("move-exception");
				} else {
					code.add(arg(insn, 0));
				}
				break;

			case TERNARY:
				break;

			case ARGS:
				code.add(arg(insn, 0));
				break;

			case NOP:
				state.add(IGState.SKIP);
				break;

			/* fallback mode instructions */
			case IF:
				assert isFallback();
				IfNode ifInsn = (IfNode) insn;
				String cond = arg(insn.getArg(0)) + " " + ifInsn.getOp().getSymbol() + " "
						+ (ifInsn.isZeroCmp() ? "0" : arg(insn.getArg(1)));
				code.add("if (").add(cond).add(") goto ").add(MethodGen.getLabelName(ifInsn.getTarget()));
				break;

			case GOTO:
				assert isFallback();
				code.add("goto ").add(MethodGen.getLabelName(((GotoNode) insn).getTarget()));
				break;

			case SWITCH:
				assert isFallback();
				SwitchNode sw = (SwitchNode) insn;
				code.add("switch(").add(arg(insn, 0)).add(") {");
				code.incIndent();
				for (int i = 0; i < sw.getCasesCount(); i++) {
					code.startLine("case " + sw.getKeys()[i]
							+ ": goto " + MethodGen.getLabelName(sw.getTargets()[i]) + ";");
				}
				code.startLine("default: goto " + MethodGen.getLabelName(sw.getDefaultCaseOffset()) + ";");
				code.decIndent();
				code.startLine('}');
				state.add(IGState.NO_SEMICOLON);
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
		code.add("new ").add(useType(insn.getResult().getType()));
		code.add('{');
		for (int i = 0; i < c; i++) {
			code.add(arg(insn, i));
			if (i + 1 < c)
				code.add(", ");
		}
		code.add('}');
	}

	private void fillArray(FillArrayOp insn, CodeWriter code) throws CodegenException {
		ArgType elType = insn.getResult().getType().getArrayElement();
		if (elType.getPrimitiveType() == null) {
			elType = elType.selectFirst();
		}
		StringBuilder str = new StringBuilder();
		switch (elType.getPrimitiveType()) {
			case BOOLEAN:
			case BYTE:
				byte[] array = (byte[]) insn.getData();
				for (byte b : array) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case SHORT:
			case CHAR:
				short[] sarray = (short[]) insn.getData();
				for (short b : sarray) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case INT:
			case FLOAT:
				int[] iarray = (int[]) insn.getData();
				for (int b : iarray) {
					str.append(TypeGen.literalToString(b, elType));
					str.append(", ");
				}
				break;
			case LONG:
			case DOUBLE:
				long[] larray = (long[]) insn.getData();
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
		code.add("new ").add(useType(elType)).add("[] { ").add(str.toString()).add(" }");
	}

	private void makeConstructor(ConstructorInsn insn, CodeWriter code, EnumSet<IGState> state)
			throws CodegenException {
		ClassNode cls = root.resolveClass(insn.getClassType());
		if (cls != null && cls.isAnonymous()) {
			// anonymous class construction
			ClassInfo parent;
			if (cls.getSuperClass() != null
					&& !cls.getSuperClass().getFullName().equals("java.lang.Object"))
				parent = cls.getSuperClass();
			else
				parent = cls.getInterfaces().get(0);

			code.add("new ").add(useClass(parent)).add("()");
			code.incIndent(2);
			new ClassGen(cls, mgen.getClassGen().getParentGen(), fallback).makeClassBody(code);
			code.decIndent(2);
		} else if (insn.isSuper()) {
			code.add("super");
			addArgs(code, insn, 0);
		} else if (insn.isThis()) {
			code.add("this");
			addArgs(code, insn, 0);
		} else if (insn.isSelf()) {
			// skip
			state.add(IGState.SKIP);
		} else {
			code.add("new ").add(useClass(insn.getClassType()));
			addArgs(code, insn, 0);
		}
	}

	private void makeInvoke(InvokeNode insn, CodeWriter code) throws CodegenException {
		MethodInfo callMth = insn.getCallMth();

		// inline method
		MethodNode callMthNode = mth.dex().resolveMethod(callMth);
		if (callMthNode != null
				&& callMthNode.getAttributes().contains(AttributeType.METHOD_INLINE)) {
			inlineMethod(callMthNode, insn, code);
			return;
		}

		int k = 0;
		InvokeType type = insn.getInvokeType();
		switch (type) {
			case DIRECT:
			case VIRTUAL:
			case INTERFACE:
				code.add(arg(insn.getArg(0))).add('.');
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
					code.add(useClass(declClass)).add('.');
				}
				break;
		}
		code.add(callMth.getName());
		if (callMthNode != null && callMthNode.isArgsOverload()) {
			int argsCount = insn.getArgsCount();
			List<ArgType> originalType = callMth.getArgumentsTypes();
			int origPos = 0;

			code.add('(');
			for (int i = k; i < argsCount; i++) {
				InsnArg arg = insn.getArg(i);
				ArgType origType = originalType.get(origPos);
				if (!arg.getType().equals(origType)) {
					code.add('(').add(useType(origType)).add(')').add(arg(arg, true));
				} else {
					code.add(arg(arg, false));
				}
				if (i < argsCount - 1) {
					code.add(", ");
				}
				origPos++;
			}
			code.add(')');
		} else {
			addArgs(code, insn, k);
		}
	}

	private void addArgs(CodeWriter code, InsnNode insn, int k) throws CodegenException {
		int argsCount = insn.getArgsCount();
		code.add('(');
		if (k < argsCount) {
			code.add(arg(insn.getArg(k), false));
			for (int i = k + 1; i < argsCount; i++) {
				code.add(", ");
				code.add(arg(insn.getArg(i), false));
			}
		}
		code.add(')');
	}

	private void inlineMethod(MethodNode callMthNode, InvokeNode insn, CodeWriter code) throws CodegenException {
		IAttribute mia = callMthNode.getAttributes().get(AttributeType.METHOD_INLINE);
		InsnNode inl = ((MethodInlineAttr) mia).getInsn();
		if (callMthNode.getMethodInfo().getArgumentsTypes().isEmpty()) {
			makeInsn(inl, code, IGState.BODY_ONLY);
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
				if (r.getRegNum() >= regs.length) {
					LOG.warn("Unknown register number {} in method call: {}, {}", r, callMthNode, mth);
				} else {
					InsnArg repl = regs[r.getRegNum()];
					if (repl == null) {
						LOG.warn("Not passed register {} in method call: {}, {}", r, callMthNode, mth);
					} else {
						inl.replaceArg(r, repl);
						toRevert.put(r, repl);
					}
				}
			}
			makeInsn(inl, code, IGState.BODY_ONLY);
			// revert changes
			for (Entry<RegisterArg, InsnArg> e : toRevert.entrySet()) {
				inl.replaceArg(e.getValue(), e.getKey());
			}
		}
	}

	private void makeArith(ArithNode insn, CodeWriter code, EnumSet<IGState> state) throws CodegenException {
		ArithOp op = insn.getOp();
		String v1 = arg(insn.getArg(0));
		String v2 = arg(insn.getArg(1));
		if (state.contains(IGState.BODY_ONLY)) {
			// wrap insn in brackets for save correct operation order
			code.add('(').add(v1).add(' ').add(op.getSymbol()).add(' ').add(v2).add(')');
		} else if (state.contains(IGState.BODY_ONLY_NOWRAP)) {
			code.add(v1).add(' ').add(op.getSymbol()).add(' ').add(v2);
		} else {
			String res = arg(insn.getResult());
			if (res.equals(v1) && insn.getResult().equals(insn.getArg(0))) {
				state.add(IGState.NO_RESULT);
				// "++" or "--"
				if (insn.getArg(1).isLiteral() && (op == ArithOp.ADD || op == ArithOp.SUB)) {
					LiteralArg lit = (LiteralArg) insn.getArg(1);
					if (Math.abs(lit.getLiteral()) == 1 && lit.isInteger()) {
						code.add(assignVar(insn)).add(op.getSymbol()).add(op.getSymbol());
						return;
					}
				}
				// +=, -= ...
				v2 = arg(insn.getArg(1), false);
				code.add(assignVar(insn)).add(' ').add(op.getSymbol()).add("= ").add(v2);
			} else {
				code.add(v1).add(' ').add(op.getSymbol()).add(' ').add(v2);
			}
		}
	}
}
