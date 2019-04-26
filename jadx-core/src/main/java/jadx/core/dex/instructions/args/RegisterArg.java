package jadx.core.dex.instructions.args;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class RegisterArg extends InsnArg implements Named {
	private static final Logger LOG = LoggerFactory.getLogger(RegisterArg.class);
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
		if (contains(AFlag.IMMUTABLE_TYPE)) {
			if (!type.isTypeKnown()) {
				throw new JadxRuntimeException("Unknown immutable type '" + type + "' in " + this);
			}
			if (!type.equals(newType)) {
				LOG.warn("JADX WARNING: Can't change immutable type from '{}' to '{}' for {}", type, newType, this);
				return;
			}
		}
		sVar.setType(newType);
	}

	@Override
	public ArgType getType() {
		if (sVar != null) {
			return sVar.getTypeInfo().getType();
		}
		LOG.warn("Register type unknown, SSA variable not initialized: r{}", regNum);
		return type;
	}

	public ArgType getInitType() {
		return type;
	}

	@Override
	public boolean isTypeImmutable() {
		return contains(AFlag.IMMUTABLE_TYPE) || (sVar != null && sVar.contains(AFlag.IMMUTABLE_TYPE));
	}

	public SSAVar getSVar() {
		return sVar;
	}

	void setSVar(@NotNull SSAVar sVar) {
		this.sVar = sVar;
		if (contains(AFlag.IMMUTABLE_TYPE)) {
			sVar.add(AFlag.IMMUTABLE_TYPE);
		}
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
			dup.setSVar(sVar);
		}
		dup.copyAttributesFrom(this);
		return dup;
	}

	/**
	 * Return constant value from register assign or null if not constant
	 */
	public Object getConstValue(DexNode dex) {
		InsnNode parInsn = getAssignInsn();
		if (parInsn == null) {
			return null;
		}
		return InsnUtils.getConstValueByInsn(dex, parInsn);
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
		if (!arg.isRegister()) {
			return false;
		}
		RegisterArg reg = (RegisterArg) arg;
		return regNum == reg.getRegNum()
				&& Objects.equals(sVar, reg.getSVar());
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
		sb.append("(r");
		sb.append(regNum);
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
