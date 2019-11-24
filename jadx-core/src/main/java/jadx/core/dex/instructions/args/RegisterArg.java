package jadx.core.dex.instructions.args;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class RegisterArg extends InsnArg implements Named {
	public static final String THIS_ARG_NAME = "this";

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

	@Override
	public void setType(ArgType newType) {
		if (sVar == null) {
			throw new JadxRuntimeException("Can't change type for register without SSA variable: " + this);
		}
		sVar.setType(newType);
	}

	@Override
	public ArgType getType() {
		if (sVar != null) {
			return sVar.getTypeInfo().getType();
		}
		return ArgType.UNKNOWN;
	}

	public ArgType getInitType() {
		return type;
	}

	@Nullable
	public ArgType getImmutableType() {
		if (contains(AFlag.IMMUTABLE_TYPE)) {
			return type;
		}
		if (sVar != null) {
			return sVar.getImmutableType();
		}
		return null;
	}

	@Override
	public boolean isTypeImmutable() {
		if (contains(AFlag.IMMUTABLE_TYPE)) {
			return true;
		}
		return sVar != null && sVar.isTypeImmutable();
	}

	public SSAVar getSVar() {
		return sVar;
	}

	void setSVar(@NotNull SSAVar sVar) {
		this.sVar = sVar;
	}

	@Override
	public void add(AFlag flag) {
		if (flag == AFlag.IMMUTABLE_TYPE && !type.isTypeKnown()) {
			throw new JadxRuntimeException("Can't mark unknown type as immutable, type: " + type + ", reg: " + this);
		}
		super.add(flag);
	}

	@Override
	public String getName() {
		if (isThis()) {
			return THIS_ARG_NAME;
		}
		if (sVar == null) {
			return null;
		}
		return sVar.getName();
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
		return duplicate(getRegNum(), sVar);
	}

	public RegisterArg duplicate(int regNum, @Nullable SSAVar sVar) {
		RegisterArg dup = new RegisterArg(regNum, getInitType());
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
