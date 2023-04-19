package jadx.core.dex.instructions.args;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.exceptions.JadxRuntimeException;

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

	public static RegisterArg reg(InsnData insn, int argNum, ArgType type) {
		return reg(insn.getReg(argNum), type);
	}

	public static RegisterArg typeImmutableIfKnownReg(InsnData insn, int argNum, ArgType type) {
		if (type.isTypeKnown()) {
			return typeImmutableReg(insn.getReg(argNum), type);
		}
		return reg(insn.getReg(argNum), type);
	}

	public static RegisterArg typeImmutableReg(InsnData insn, int argNum, ArgType type) {
		return typeImmutableReg(insn.getReg(argNum), type);
	}

	public static RegisterArg typeImmutableReg(int regNum, ArgType type) {
		return reg(regNum, type, true);
	}

	public static RegisterArg reg(int regNum, ArgType type, boolean typeImmutable) {
		RegisterArg reg = new RegisterArg(regNum, type);
		if (typeImmutable) {
			reg.add(AFlag.IMMUTABLE_TYPE);
		}
		return reg;
	}

	public static LiteralArg lit(long literal, ArgType type) {
		return LiteralArg.makeWithFixedType(literal, type);
	}

	public static LiteralArg lit(InsnData insn, ArgType type) {
		return lit(insn.getLiteral(), type);
	}

	private static InsnWrapArg wrap(InsnNode insn) {
		insn.add(AFlag.WRAPPED);
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

	@Nullable
	public InsnNode getParentInsn() {
		return parentInsn;
	}

	public void setParentInsn(@Nullable InsnNode parentInsn) {
		this.parentInsn = parentInsn;
	}

	@Nullable("if wrap failed")
	public InsnArg wrapInstruction(MethodNode mth, InsnNode insn) {
		return wrapInstruction(mth, insn, true);
	}

	@Nullable("if wrap failed")
	public InsnArg wrapInstruction(MethodNode mth, InsnNode insn, boolean unbind) {
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
		if (insn.getType() == InsnType.MOVE && this.isRegister()) {
			// preserve variable name for move insn (needed in `for-each` loop for iteration variable)
			String name = ((RegisterArg) this).getName();
			if (name != null) {
				InsnArg arg = insn.getArg(0);
				if (arg.isRegister()) {
					((RegisterArg) arg).setNameIfUnknown(name);
				} else if (arg.isInsnWrap()) {
					InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
					RegisterArg registerArg = wrapInsn.getResult();
					if (registerArg != null) {
						registerArg.setNameIfUnknown(name);
					}
				}
			}
		}
		InsnArg arg = wrapInsnIntoArg(insn);
		InsnArg oldArg = parent.getArg(i);
		parent.setArg(i, arg);
		InsnRemover.unbindArgUsage(mth, oldArg);
		if (unbind) {
			InsnRemover.unbindArgUsage(mth, this);
			// result not needed in wrapped insn
			InsnRemover.unbindResult(mth, insn);
			insn.setResult(null);
		}
		return arg;
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

	@NotNull
	public static InsnArg wrapInsnIntoArg(InsnNode insn) {
		InsnType type = insn.getType();
		if (type == InsnType.CONST || type == InsnType.MOVE) {
			if (insn.contains(AFlag.FORCE_ASSIGN_INLINE)) {
				RegisterArg resArg = insn.getResult();
				InsnArg arg = wrap(insn);
				if (resArg != null) {
					arg.setType(resArg.getType());
				}
				return arg;
			} else {
				InsnArg arg = insn.getArg(0);
				insn.add(AFlag.DONT_GENERATE);
				return arg;
			}
		}
		return wrapArg(insn);
	}

	/**
	 * Prefer {@link InsnArg#wrapInsnIntoArg(InsnNode)}.
	 * <p>
	 * This method don't support MOVE and CONST insns!
	 */
	public static InsnArg wrapArg(InsnNode insn) {
		RegisterArg resArg = insn.getResult();
		InsnArg arg = wrap(insn);
		switch (insn.getType()) {
			case CONST:
			case MOVE:
				throw new JadxRuntimeException("Don't wrap MOVE or CONST insns: " + insn);

			case CONST_STR:
				arg.setType(ArgType.STRING);
				if (resArg != null) {
					resArg.setType(ArgType.STRING);
				}
				break;
			case CONST_CLASS:
				arg.setType(ArgType.CLASS);
				if (resArg != null) {
					resArg.setType(ArgType.CLASS);
				}
				break;

			default:
				if (resArg != null) {
					arg.setType(resArg.getType());
				}
				break;
		}
		return arg;
	}

	public boolean isZeroLiteral() {
		return isLiteral() && (((LiteralArg) this)).getLiteral() == 0;
	}

	public boolean isFalse() {
		if (isLiteral()) {
			LiteralArg litArg = (LiteralArg) this;
			return litArg.getLiteral() == 0 && Objects.equals(litArg.getType(), ArgType.BOOLEAN);
		}
		return false;
	}

	public boolean isTrue() {
		if (isLiteral()) {
			LiteralArg litArg = (LiteralArg) this;
			return litArg.getLiteral() == 1 && Objects.equals(litArg.getType(), ArgType.BOOLEAN);
		}
		return false;
	}

	public boolean isThis() {
		return contains(AFlag.THIS);
	}

	/**
	 * Return true for 'this' from other classes (often occur in anonymous classes)
	 */
	public boolean isAnyThis() {
		if (contains(AFlag.THIS)) {
			return true;
		}
		InsnNode wrappedInsn = unwrap();
		if (wrappedInsn != null && wrappedInsn.getType() == InsnType.IGET) {
			return wrappedInsn.getArg(0).isAnyThis();
		}
		return false;
	}

	public InsnNode unwrap() {
		if (isInsnWrap()) {
			return ((InsnWrapArg) this).getWrapInsn();
		}
		return null;
	}

	public boolean isConst() {
		return isLiteral() || (isInsnWrap() && ((InsnWrapArg) this).getWrapInsn().isConstInsn());
	}

	public boolean isSameConst(InsnArg other) {
		if (isConst() && other.isConst()) {
			return this.equals(other);
		}
		return false;
	}

	public boolean isSameVar(RegisterArg arg) {
		if (isRegister()) {
			return ((RegisterArg) this).sameRegAndSVar(arg);
		}
		return false;
	}

	protected final <T extends InsnArg> T copyCommonParams(T copy) {
		copy.copyAttributesFrom(this);
		copy.setParentInsn(parentInsn);
		return copy;
	}

	public InsnArg duplicate() {
		return this;
	}
}
