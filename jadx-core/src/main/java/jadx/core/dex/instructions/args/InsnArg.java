package jadx.core.dex.instructions.args;

import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.io.instructions.DecodedInstruction;

/**
 * Instruction argument,
 * argument can be register, literal or instruction
 */
public abstract class InsnArg extends Typed {

	private static final Logger LOG = LoggerFactory.getLogger(InsnArg.class);

	protected InsnNode parentInsn;

	public static RegisterArg reg(int regNum, ArgType type) {
		return new RegisterArg(regNum, type);
	}

	public static RegisterArg reg(DecodedInstruction insn, int argNum, ArgType type) {
		return reg(InsnUtils.getArg(insn, argNum), type);
	}

	public static RegisterArg parameterReg(int regNum, ArgType type) {
		return new MthParameterArg(regNum, type);
	}

	public static LiteralArg lit(long literal, ArgType type) {
		return new LiteralArg(literal, type);
	}

	public static LiteralArg lit(DecodedInstruction insn, ArgType type) {
		return lit(insn.getLiteral(), type);
	}

	private static InsnWrapArg wrap(InsnNode insn) {
		return new InsnWrapArg(insn);
	}

	public boolean isRegister() {
		return false;
	}

	public boolean isLiteral() {
		return false;
	}

	public boolean isInsnWrap() {
		return false;
	}

	public boolean isNamed() {
		return false;
	}

	public boolean isField() {
		return false;
	}

	public InsnNode getParentInsn() {
		return parentInsn;
	}

	public void setParentInsn(InsnNode parentInsn) {
		this.parentInsn = parentInsn;
	}

	public InsnArg wrapInstruction(InsnNode insn) {
		InsnNode parent = parentInsn;
		if (parent == null) {
			return null;
		}
		if (parent == insn) {
			LOG.debug("Can't wrap instruction info itself: " + insn);
			return null;
		}
		int count = parent.getArgsCount();
		for (int i = 0; i < count; i++) {
			if (parent.getArg(i) == this) {
				InsnArg arg = wrapArg(insn);
				parent.setArg(i, arg);
				return arg;
			}
		}
		return null;
	}

	public static InsnArg wrapArg(InsnNode insn) {
		InsnArg arg;
		switch (insn.getType()) {
			case MOVE:
			case CONST:
				arg = insn.getArg(0);
				break;
			case CONST_STR:
				arg = wrap(insn);
				arg.setType(ArgType.STRING);
				break;
			case CONST_CLASS:
				arg = wrap(insn);
				arg.setType(ArgType.CLASS);
				break;
			default:
				arg = wrap(insn);
				break;
		}
		return arg;
	}

	public boolean isThis() {
		// must be implemented in RegisterArg
		return false;
	}
}
