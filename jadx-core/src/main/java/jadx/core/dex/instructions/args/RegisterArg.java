package jadx.core.dex.instructions.args;

import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.parser.FieldValueAttr;
import jadx.core.dex.visitors.InstructionRemover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterArg extends InsnArg {
	private static final Logger LOG = LoggerFactory.getLogger(RegisterArg.class);

	protected final int regNum;

	public RegisterArg(int rn) {
		this.regNum = rn;
	}

	public RegisterArg(int rn, ArgType type) {
		this.typedVar = new TypedVar(type);
		this.regNum = rn;
	}

	public int getRegNum() {
		return regNum;
	}

	@Override
	public boolean isRegister() {
		return true;
	}

	public InsnNode getAssignInsn() {
		for (InsnArg arg : getTypedVar().getUseList()) {
			InsnNode assignInsn = arg.getParentInsn();
			if (assignInsn == null)
				// assign as function argument
				return null;
			else if (assignInsn.getResult() != null
					&& assignInsn.getResult().getRegNum() == regNum)
				return assignInsn;
		}
		return null;
	}

	/**
	 * Return constant value from register assign or null if not constant
	 *
	 * @return LiteralArg, String or ArgType
	 */
	public Object getConstValue(DexNode dex) {
		InsnNode parInsn = getAssignInsn();
		if (parInsn != null) {
			InsnType insnType = parInsn.getType();
			switch (insnType) {
				case CONST:
					return parInsn.getArg(0);
				case CONST_STR:
					return ((ConstStringNode) parInsn).getString();
				case CONST_CLASS:
					return ((ConstClassNode) parInsn).getClsType();
				case SGET:
					FieldInfo f = (FieldInfo) ((IndexInsnNode) parInsn).getIndex();
					FieldNode fieldNode = dex.resolveField(f);
					if (fieldNode != null) {
						FieldValueAttr attr = (FieldValueAttr) fieldNode.getAttributes().get(AttributeType.FIELD_VALUE);
						if (attr != null) {
							return attr.getValue();
						}
					} else {
						LOG.warn("Field {} not found in dex {}", f, dex);
					}
					break;
			}
		}
		return null;
	}

	@Override
	public boolean isThis() {
		if (isRegister()) {
			String name = getTypedVar().getName();
			if (name != null && name.equals("this")) {
				return true;
			}
			// maybe it was moved from 'this' register
			InsnNode ai = getAssignInsn();
			if (ai != null && ai.getType() == InsnType.MOVE) {
				InsnArg arg = ai.getArg(0);
				if (arg != this && arg.isThis()) {
					// actually we need to remove this instruction but we can't
					// because of iterating on instructions list
					// so unbind insn and rely on code shrinker
					InstructionRemover.unbindInsn(ai);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return regNum * 31 + typedVar.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RegisterArg other = (RegisterArg) obj;
		if (regNum != other.regNum) return false;
		if (!typedVar.equals(other.typedVar)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "(r" + regNum + " " + typedVar + ")";
	}
}
