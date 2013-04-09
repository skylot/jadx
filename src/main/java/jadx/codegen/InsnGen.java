package jadx.codegen;

import jadx.dex.attributes.AttributeType;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.FieldInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.ArithNode;
import jadx.dex.instructions.ArithOp;
import jadx.dex.instructions.FillArrayOp;
import jadx.dex.instructions.GotoNode;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.IndexInsnNode;
import jadx.dex.instructions.InvokeNode;
import jadx.dex.instructions.InvokeType;
import jadx.dex.instructions.SwitchNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.InsnWrapArg;
import jadx.dex.instructions.args.LiteralArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.instructions.mods.ConstructorInsn;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.FieldNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.nodes.RootNode;
import jadx.utils.StringUtils;
import jadx.utils.exceptions.CodegenException;

import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(InsnGen.class);

	protected final MethodGen mgen;
	protected final MethodNode mth;
	protected final RootNode root;
	private final boolean fallback;

	public enum InsnGenState {
		SKIP,

		NO_SEMICOLON,
		NO_RESULT,

		BODY_ONLY,

		INC_INDENT,
		DEC_INDENT,
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
		if (arg.isRegister()) {
			return mgen.makeArgName((RegisterArg) arg);
		} else if (arg.isLiteral()) {
			return lit((LiteralArg) arg);
		} else {
			CodeWriter code = new CodeWriter();
			makeInsn(((InsnWrapArg) arg).getWrapInsn(), code, true);
			return code.toString();
		}
	}

	public String assignVar(InsnNode insn) {
		try {
			RegisterArg arg = insn.getResult();
			if (insn.getAttributes().contains(AttributeType.DECLARE_VARIABLE)) {
				return declareVar(arg);
			} else {
				return arg(arg);
			}
		} catch (CodegenException e) {
			LOG.error("Assign var codegen error", e);
		}
		return "<error>";
	}

	public String declareVar(RegisterArg arg) throws CodegenException {
		String type = TypeGen.translate(mgen.getClassGen(), arg.getType());
		String generic = arg.getType().getGeneric();
		if (generic != null)
			type += " /* " + generic + " */";
		return type + " " + arg(arg);
	}

	private String lit(LiteralArg arg) {
		return TypeGen.literalToString(arg.getLiteral(), arg.getType());
	}

	private String ifield(IndexInsnNode insn, int reg) throws CodegenException {
		FieldInfo field = (FieldInfo) insn.getIndex();
		return arg(insn.getArg(reg)) + '.' + field.getName();
	}

	private String sfield(IndexInsnNode insn) {
		FieldInfo field = (FieldInfo) insn.getIndex();
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
		if (type.isObject())
			return mgen.getClassGen().useClass(type);
		else
			return translate(type);
	}

	private String translate(ArgType type) {
		return TypeGen.translate(mgen.getClassGen(), type);
	}

	public void makeInsn(InsnNode insn, CodeWriter code) throws CodegenException {
		makeInsn(insn, code, false);
	}

	private void makeInsn(InsnNode insn, CodeWriter code, boolean bodyOnly) throws CodegenException {
		try {
			// code.startLine("/* " + insn + "*/");
			EnumSet<InsnGenState> state = EnumSet.noneOf(InsnGenState.class);
			if (bodyOnly) {
				state.add(InsnGenState.BODY_ONLY);
				makeInsnBody(code, insn, state);
			} else {
				CodeWriter body = new CodeWriter(code.getIndent());
				makeInsnBody(body, insn, state);
				if (state.contains(InsnGenState.SKIP))
					return;

				if (state.contains(InsnGenState.DEC_INDENT))
					code.decIndent();
				if (insn.getResult() != null && !state.contains(InsnGenState.NO_RESULT))
					code.startLine(assignVar(insn)).add(" = ");
				else
					code.startLine();

				code.add(body);

				if (!state.contains(InsnGenState.NO_SEMICOLON))
					code.add(';');
				if (state.contains(InsnGenState.INC_INDENT))
					code.incIndent();
			}
		} catch (Throwable th) {
			throw new CodegenException(mth, "Error generate insn: " + insn, th);
		}
	}

	private void makeInsnBody(CodeWriter code, InsnNode insn, EnumSet<InsnGenState> state) throws CodegenException {
		switch (insn.getType()) {
			case CONST:
				if (insn.getArgsCount() == 0) {
					// const in 'index' - string or class
					Object ind = ((IndexInsnNode) insn).getIndex();
					if (ind instanceof String)
						code.add(StringUtils.unescapeString(ind.toString()));
					else if (ind instanceof ArgType)
						code.add(useType((ArgType) ind)).add(".class");
				} else {
					LiteralArg arg = (LiteralArg) insn.getArg(0);
					code.add(lit(arg));
				}
				break;

			case MOVE:
				code.add(arg(insn.getArg(0)));
				break;

			case CHECK_CAST:
			case CAST:
				code.add("((");
				code.add(translate(((ArgType) ((IndexInsnNode) insn).getIndex())));
				code.add(") (");
				code.add(arg(insn.getArg(0)));
				code.add("))");
				break;

			case ARITH:
				makeArith((ArithNode) insn, code, state);
				break;

			case NEG:
				String base = "-" + arg(insn.getArg(0));
				if (state.contains(InsnGenState.BODY_ONLY))
					code.add('(').add(base).add(')');
				else
					code.add(base);
				break;

			case RETURN:
				if (insn.getArgsCount() != 0)
					code.add("return " + arg(insn.getArg(0)));
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
				code.add("throw " + arg(insn.getArg(0)));
				break;

			case CMP_L:
			case CMP_G:
				code.add(String.format("(%1$s > %2$s ? 1 : (%1$s == %2$s ? 0 : -1))", arg(insn, 0), arg(insn, 1)));
				break;

			case INSTANCE_OF:
				code.add("(").add(arg(insn, 0)).add(" instanceof ")
						.add(useType((ArgType) ((IndexInsnNode) insn).getIndex())).add(")");
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
				code.add(arg(insn.getArg(0))).add('[').add(arg(insn.getArg(1))).add(']');
				break;

			case APUT:
				code.add(arg(insn, 0)).add('[').add(arg(insn, 1)).add("] = ").add(arg(insn, 2));
				break;

			case IGET:
				code.add(ifield((IndexInsnNode) insn, 0));
				break;
			case IPUT:
				code.add(ifield((IndexInsnNode) insn, 1) + " = " + arg(insn.getArg(0)));
				break;

			case SGET:
				code.add(sfield((IndexInsnNode) insn));
				break;
			case SPUT:
				IndexInsnNode node = (IndexInsnNode) insn;
				fieldPut(node);
				code.add(sfield(node) + " = " + arg(node.getArg(0)));
				break;

			case MONITOR_ENTER:
				if (isFallback()) {
					code.add("monitor-enter(").add(arg(insn.getArg(0))).add(")");
				} else {
					state.add(InsnGenState.SKIP);
				}
				break;

			case MONITOR_EXIT:
				if (isFallback()) {
					code.add("monitor-exit(").add(arg(insn.getArg(0))).add(")");
				} else {
					state.add(InsnGenState.SKIP);
				}
				break;

			case MOVE_EXCEPTION:
				if (isFallback()) {
					code.add("move-exception");
				} else {
					// don't have body
					if (state.contains(InsnGenState.BODY_ONLY))
						code.add(arg(insn.getResult()));
					else
						state.add(InsnGenState.SKIP);
				}
				break;

			case TERNARY:
				break;

			/* fallback mode instructions */
			case NOP:
				state.add(InsnGenState.SKIP);
				break;

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
				code.startLine("}");
				state.add(InsnGenState.NO_SEMICOLON);
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
		code.add("{");
		for (int i = 0; i < c; i++) {
			code.add(arg(insn, i));
			if (i + 1 < c)
				code.add(", ");
		}
		code.add("}");
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
		code.add("new ").add(translate(elType)).add("[] { ").add(str.toString()).add(" }");
	}

	private void makeConstructor(ConstructorInsn insn, CodeWriter code, EnumSet<InsnGenState> state)
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
			state.add(InsnGenState.SKIP);
		} else {
			code.add("new ").add(useClass(insn.getClassType()));
			addArgs(code, insn, 0);
		}
	}

	private void makeInvoke(InvokeNode insn, CodeWriter code) throws CodegenException {
		MethodInfo callMth = insn.getCallMth();

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
				if (!insnCls.equals(callMth.getDeclClass()))
					code.add(useClass(callMth.getDeclClass())).add('.');
				break;
		}
		code.add(callMth.getName());
		addArgs(code, insn, k);
	}

	private void addArgs(CodeWriter code, InsnNode insn, int k) throws CodegenException {
		code.add('(');
		for (int i = k; i < insn.getArgsCount(); i++) {
			code.add(arg(insn.getArg(i)));
			if (i < insn.getArgsCount() - 1)
				code.add(", ");
		}
		code.add(")");
	}

	private void makeArith(ArithNode insn, CodeWriter code, EnumSet<InsnGenState> state) throws CodegenException {
		ArithOp op = insn.getOp();
		String v1 = arg(insn.getArg(0));

		if (op == ArithOp.INC || op == ArithOp.DEC) {
			code.add(v1 + op.getSymbol());
		} else {
			String res = arg(insn.getResult());
			String v2 = arg(insn.getArg(1));
			if (res.equals(v1) && !state.contains(InsnGenState.BODY_ONLY)) {
				code.add(assignVar(insn) + " " + op.getSymbol() + "= " + v2);
				state.add(InsnGenState.NO_RESULT);
			} else {
				if (state.contains(InsnGenState.BODY_ONLY))
					// wrap insn in brackets for save correct operation order
					// TODO don't wrap first insn in wrapped stack
					code.add("(" + v1 + " " + op.getSymbol() + " " + v2 + ")");
				else
					code.add(v1 + " " + op.getSymbol() + " " + v2);
			}
		}
	}

}
