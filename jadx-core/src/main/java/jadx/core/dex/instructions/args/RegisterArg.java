package jadx.core.dex.instructions.args;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class RegisterArg extends InsnArg implements Named {
	public static final String THIS_ARG_NAME = "this";
	public static final String SUPER_ARG_NAME = "super";

	protected final int regNum;
	// not null after SSATransform pass
	private SSAVar sVar;

	public RegisterArg(int rn, ArgType type) {
		this.type = type; // initial type, not changing, can be unknown
		this.regNum = rn;
	}

	public int getRegNum() {
		return regNum;
	}

	@Override
	public boolean isRegister() {
		return true;
	}

	public ArgType getInitType() {
		return type;
	}

	@Override
	public ArgType getType() {
		if (sVar != null) {
			return sVar.getTypeInfo().getType();
		}
		return ArgType.UNKNOWN;
	}

	@Override
	public void setType(ArgType newType) {
		if (sVar == null) {
			throw new JadxRuntimeException("Can't change type for register without SSA variable: " + this);
		}
		sVar.setType(newType);
	}

	public void forceSetInitType(ArgType type) {
		this.type = type;
	}

	@Nullable
	public ArgType getImmutableType() {
		if (sVar != null) {
			return sVar.getImmutableType();
		}
		if (contains(AFlag.IMMUTABLE_TYPE)) {
			return type;
		}
		return null;
	}

	@Override
	public boolean isTypeImmutable() {
		if (sVar != null) {
			return sVar.isTypeImmutable();
		}
		return contains(AFlag.IMMUTABLE_TYPE);
	}

	public SSAVar getSVar() {
		return sVar;
	}

	void setSVar(@NotNull SSAVar sVar) {
		this.sVar = sVar;
	}

	public void resetSSAVar() {
		this.sVar = null;
	}

	@Override
	public String getName() {
		if (isSuper()) {
			return SUPER_ARG_NAME;
		}
		if (isThis()) {
			return THIS_ARG_NAME;
		}
		if (sVar == null) {
			return null;
		}
		return sVar.getName();
	}

	private boolean isSuper() {
		return contains(AFlag.SUPER);
	}

	@Override
	public void setName(String name) {
		if (sVar != null && name != null) {
			sVar.setName(name);
		}
	}

	public void setNameIfUnknown(String name) {
		if (getName() == null) {
			setName(name);
		}
	}

	public boolean isNameEquals(InsnArg arg) {
		String n = getName();
		if (n == null || !(arg instanceof Named)) {
			return false;
		}
		return n.equals(((Named) arg).getName());
	}

	@Override
	public RegisterArg duplicate() {
		return duplicate(getRegNum(), getInitType(), sVar);
	}

	public RegisterArg duplicate(ArgType initType) {
		return duplicate(getRegNum(), initType, sVar);
	}

	public RegisterArg duplicateWithNewSSAVar(MethodNode mth) {
		RegisterArg duplicate = duplicate(regNum, getInitType(), null);
		mth.makeNewSVar(duplicate);
		return duplicate;
	}

	public RegisterArg duplicate(int regNum, @Nullable SSAVar sVar) {
		return duplicate(regNum, getInitType(), sVar);
	}

	public RegisterArg duplicate(int regNum, ArgType initType, @Nullable SSAVar sVar) {
		RegisterArg dup = new RegisterArg(regNum, initType);
		if (sVar != null) {
			// only 'set' here, 'assign' or 'use' will binds later
			dup.setSVar(sVar);
		}
		return copyCommonParams(dup);
	}

	@Nullable
	public InsnNode getAssignInsn() {
		if (sVar == null) {
			return null;
		}
		return sVar.getAssign().getParentInsn();
	}

	public boolean equalRegisterAndType(RegisterArg arg) {
		return regNum == arg.regNum && type.equals(arg.type);
	}

	public boolean sameRegAndSVar(InsnArg arg) {
		if (this == arg) {
			return true;
		}
		if (!arg.isRegister()) {
			return false;
		}
		RegisterArg reg = (RegisterArg) arg;
		return regNum == reg.getRegNum()
				&& Objects.equals(sVar, reg.getSVar());
	}

	public boolean sameReg(InsnArg arg) {
		if (!arg.isRegister()) {
			return false;
		}
		return regNum == ((RegisterArg) arg).getRegNum();
	}

	public boolean sameCodeVar(RegisterArg arg) {
		return this.getSVar().getCodeVar() == arg.getSVar().getCodeVar();
	}

	public boolean isLinkedToOtherSsaVars() {
		return getSVar().getCodeVar().getSsaVars().size() > 1;
	}

	@Override
	public int hashCode() {
		return regNum;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RegisterArg)) {
			return false;
		}
		RegisterArg other = (RegisterArg) obj;
		return regNum == other.regNum
				&& Objects.equals(sVar, other.getSVar());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(r").append(regNum);
		if (sVar != null) {
			sb.append('v').append(sVar.getVersion());
		}
		if (getName() != null) {
			sb.append(" '").append(getName()).append('\'');
		}
		ArgType type = sVar != null ? getType() : null;
		if (type != null) {
			sb.append(' ').append(type);
		}
		ArgType initType = getInitType();
		if (type == null || (!type.equals(initType) && !type.isTypeKnown())) {
			sb.append(" I:").append(initType);
		}
		if (!isAttrStorageEmpty()) {
			sb.append(' ').append(getAttributesString());
		}
		sb.append(')');
		return sb.toString();
	}
}
