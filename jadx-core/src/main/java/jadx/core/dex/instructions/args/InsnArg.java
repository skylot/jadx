package jadx.core.dex.instructions.args;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import com.android.dx.io.instructions.DecodedInstruction;

/**
 * Instruction argument,
 * argument can be register, literal or instruction
 */
public abstract class InsnArg extends Typed {

	protected InsnNode parentInsn;

	public static RegisterArg reg(int regNum, ArgType type) {
		return new RegisterArg(regNum, type);
	}

	public static RegisterArg reg(DecodedInstruction insn, int argNum, ArgType type) {
		return reg(InsnUtils.getArg(insn, argNum), type);
	}

	public static LiteralArg lit(long literal, ArgType type) {
		return new LiteralArg(literal, type);
	}

	public static LiteralArg lit(DecodedInstruction insn, ArgType type) {
		return lit(insn.getLiteral(), type);
	}

	public static InsnWrapArg wrap(InsnNode insn) {
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
		assert parentInsn != insn : "Can't wrap instruction info itself";
		int count = parentInsn.getArgsCount();
		for (int i = 0; i < count; i++) {
			if (parentInsn.getArg(i) == this) {
				InsnArg arg;
				InsnType insnType = insn.getType();
				switch (insnType) {
					case MOVE:
					case CONST:
						arg = insn.getArg(0);
						String name = insn.getResult().getTypedVar().getName();
						if (name != null) {
							arg.getTypedVar().setName(name);
						}
						break;
					case CONST_STR:
						arg = wrap(insn);
						arg.getTypedVar().forceSetType(ArgType.STRING);
						break;
					case CONST_CLASS:
						arg = wrap(insn);
						arg.getTypedVar().forceSetType(ArgType.CLASS);
						break;
					default:
						arg = wrap(insn);
						break;
				}
				parentInsn.setArg(i, arg);
				return arg;
			}
		}
		return null;
	}

	public boolean isThis() {
		// must be implemented in RegisterArg
		return false;
	}
}
