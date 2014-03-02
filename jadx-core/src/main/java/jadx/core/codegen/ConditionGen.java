package jadx.core.codegen;

import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.regions.Compare;
import jadx.core.dex.regions.IfCondition;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.CodegenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionGen {
	private static final Logger LOG = LoggerFactory.getLogger(ConditionGen.class);

	static String make(InsnGen insnGen, IfCondition condition) throws CodegenException {
		switch (condition.getMode()) {
			case COMPARE:
				return makeCompare(insnGen, condition.getCompare());
			case NOT:
				return "!(" + make(insnGen, condition.getArgs().get(0)) + ")";
			case AND:
			case OR:
				String mode = condition.getMode() == IfCondition.Mode.AND ? " && " : " || ";
				StringBuilder sb = new StringBuilder();
				for (IfCondition arg : condition.getArgs()) {
					if (sb.length() != 0) {
						sb.append(mode);
					}
					String s = make(insnGen, arg);
					if (arg.isCompare()) {
						sb.append(s);
					} else {
						sb.append('(').append(s).append(')');
					}
				}
				return sb.toString();
			default:
				return "??" + condition;
		}
	}

	private static String makeCompare(InsnGen insnGen, Compare compare) throws CodegenException {
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
				return insnGen.arg(firstArg, false).toString();
			} else if (op == IfOp.NE) {
				// != true
				if (isWrapNeeded(firstArg)) {
					return "!(" + insnGen.arg(firstArg) + ")";
				} else {
					return "!" + insnGen.arg(firstArg);
				}
			}
			LOG.warn(ErrorsCounter.formatErrorMsg(insnGen.mth, "Unsupported boolean condition " + op.getSymbol()));
		}
		return insnGen.arg(firstArg, isWrapNeeded(firstArg))
				+ " " + op.getSymbol() + " "
				+ insnGen.arg(secondArg, isWrapNeeded(secondArg));
	}

	private static boolean isWrapNeeded(InsnArg arg) {
		if (!arg.isInsnWrap()) {
			return false;
		}
		InsnNode insn = ((InsnWrapArg) arg).getWrapInsn();
		if (insn.getType() == InsnType.ARITH) {
			switch (((ArithNode) insn).getOp()) {
				case ADD:
				case SUB:
				case MUL:
				case DIV:
				case REM:
					return false;
			}
		} else if (insn.getType() == InsnType.INVOKE) {
			return false;
		}
		return true;
	}
}
