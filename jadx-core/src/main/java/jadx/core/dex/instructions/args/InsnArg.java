package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.List;

import com.android.dx.io.instructions.DecodedInstruction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

/**
 * Instruction argument,
 * argument can be register, literal or instruction
 */
public abstract class InsnArg extends Typed {

	private static final Logger LOG = LoggerFactory.getLogger(InsnArg.class);

	@Nullable("Null for method arguments")
	protected InsnNode parentInsn;

	public static RegisterArg reg(int regNum, ArgType type) {
		return new RegisterArg(regNum, type);
	}

	public static RegisterArg reg(DecodedInstruction insn, int argNum, ArgType type) {
		return reg(InsnUtils.getArg(insn, argNum), type);
	}

	public static TypeImmutableArg typeImmutableReg(int regNum, ArgType type) {
		return new TypeImmutableArg(regNum, type);
	}

	public static RegisterArg reg(int regNum, ArgType type, boolean typeImmutable) {
		return typeImmutable ? new TypeImmutableArg(regNum, type) : new RegisterArg(regNum, type);
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

	@Nullable
	public InsnNode getParentInsn() {
		return parentInsn;
	}

	public void setParentInsn(@Nullable InsnNode parentInsn) {
		this.parentInsn = parentInsn;
	}

	public InsnArg wrapInstruction(InsnNode insn) {
		InsnNode parent = parentInsn;
		if (parent == null) {
			return null;
		}
		if (parent == insn) {
			LOG.debug("Can't wrap instruction info itself: {}", insn);
			return null;
		}
		int i = getArgIndex(parent, this);
		if (i == -1) {
			return null;
		}
		insn.add(AFlag.WRAPPED);
		InsnArg arg = wrapArg(insn);
		parent.setArg(i, arg);
		return arg;
	}

	public static void updateParentInsn(InsnNode fromInsn, InsnNode toInsn) {
		List<RegisterArg> args = new ArrayList<>();
		fromInsn.getRegisterArgs(args);
		for (RegisterArg reg : args) {
			reg.setParentInsn(toInsn);
		}
	}

	private static int getArgIndex(InsnNode parent, InsnArg arg) {
		int count = parent.getArgsCount();
		for (int i = 0; i < count; i++) {
			if (parent.getArg(i) == arg) {
				return i;
			}
		}
		return -1;
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
		// must be implemented in RegisterArg and MthParameterArg
		return false;
	}
}
