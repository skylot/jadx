package jadx.core.codegen;

import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.conditions.Compare;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfCondition.Mode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionGen extends InsnGen {
	private static final Logger LOG = LoggerFactory.getLogger(ConditionGen.class);

	private static class CondStack {
		private final Queue<IfCondition> stack = new LinkedList<IfCondition>();

		public Queue<IfCondition> getStack() {
			return stack;
		}

		public void push(IfCondition cond) {
			stack.add(cond);
		}

		public IfCondition pop() {
			return stack.poll();
		}
	}

	public ConditionGen(InsnGen insnGen) {
		super(insnGen.mgen, insnGen.fallback);
	}

	void add(CodeWriter code, IfCondition condition) throws CodegenException {
		add(code, new CondStack(), condition);
	}

	void wrap(CodeWriter code, IfCondition condition) throws CodegenException {
		wrap(code, new CondStack(), condition);
	}

	private void add(CodeWriter code, CondStack stack, IfCondition condition) throws CodegenException {
		stack.push(condition);
		switch (condition.getMode()) {
			case COMPARE:
				addCompare(code, stack, condition.getCompare());
				break;

			case TERNARY:
				addTernary(code, stack, condition);
				break;

			case NOT:
				addNot(code, stack, condition);
				break;

			case AND:
			case OR:
				addAndOr(code, stack, condition);
				break;

			default:
				throw new JadxRuntimeException("Unknown condition mode: " + condition.getMode());
		}
		stack.pop();
	}

	private void wrap(CodeWriter code, CondStack stack, IfCondition cond) throws CodegenException {
		boolean wrap = isWrapNeeded(cond);
		if (wrap) {
			code.add('(');
		}
		add(code, stack, cond);
		if (wrap) {
			code.add(')');
		}
	}

	private void wrap(CodeWriter code, InsnArg firstArg) throws CodegenException {
		boolean wrap = isArgWrapNeeded(firstArg);
		if (wrap) {
			code.add('(');
		}
		addArg(code, firstArg, false);
		if (wrap) {
			code.add(')');
		}
	}

	private void addCompare(CodeWriter code, CondStack stack, Compare compare) throws CodegenException {
		IfOp op = compare.getOp();
		InsnArg firstArg = compare.getA();
		InsnArg secondArg = compare.getB();
		if (firstArg.getType().equals(ArgType.BOOLEAN)
				&& secondArg.isLiteral()
				&& secondArg.getType().equals(ArgType.BOOLEAN)) {
			LiteralArg lit = (LiteralArg) secondArg;
			if (lit.getLiteral() == 0) {
				op = op.invert();
			}
			if (op == IfOp.EQ) {
				// == true
				if (stack.getStack().size() == 1) {
					addArg(code, firstArg, false);
				} else {
					wrap(code, firstArg);
				}
				return;
			} else if (op == IfOp.NE) {
				// != true
				code.add('!');
				wrap(code, firstArg);
				return;
			}
			LOG.warn(ErrorsCounter.formatErrorMsg(mth, "Unsupported boolean condition " + op.getSymbol()));
		}

		addArg(code, firstArg, isArgWrapNeeded(firstArg));
		code.add(' ').add(op.getSymbol()).add(' ');
		addArg(code, secondArg, isArgWrapNeeded(secondArg));
	}

	private void addTernary(CodeWriter code, CondStack stack, IfCondition condition) throws CodegenException {
		add(code, stack, condition.first());
		code.add(" ? ");
		add(code, stack, condition.second());
		code.add(" : ");
		add(code, stack, condition.third());
	}

	private void addNot(CodeWriter code, CondStack stack, IfCondition condition) throws CodegenException {
		code.add('!');
		wrap(code, stack, condition.getArgs().get(0));
	}

	private void addAndOr(CodeWriter code, CondStack stack, IfCondition condition) throws CodegenException {
		String mode = condition.getMode() == Mode.AND ? " && " : " || ";
		Iterator<IfCondition> it = condition.getArgs().iterator();
		while (it.hasNext()) {
			wrap(code, stack, it.next());
			if (it.hasNext()) {
				code.add(mode);
			}
		}
	}

	private boolean isWrapNeeded(IfCondition condition) {
		if (condition.isCompare()) {
			return false;
		}
		return condition.getMode() != Mode.NOT;
	}

	private static boolean isArgWrapNeeded(InsnArg arg) {
		if (!arg.isInsnWrap()) {
			return false;
		}
		InsnNode insn = ((InsnWrapArg) arg).getWrapInsn();
		InsnType insnType = insn.getType();
		if (insnType == InsnType.ARITH) {
			switch (((ArithNode) insn).getOp()) {
				case ADD:
				case SUB:
				case MUL:
				case DIV:
				case REM:
					return false;
			}
		} else {
			switch (insnType) {
				case INVOKE:
				case SGET:
				case IGET:
				case AGET:
				case CONST:
				case ARRAY_LENGTH:
					return false;
				default:
					return true;
			}
		}
		return true;
	}
}
