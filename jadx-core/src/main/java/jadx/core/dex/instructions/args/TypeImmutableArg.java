package jadx.core.dex.instructions.args;

import java.util.Objects;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeImmutableArg extends RegisterArg {

	public TypeImmutableArg(int rn, ArgType type) {
		super(rn, type);
	}

	@Override
	public boolean isTypeImmutable() {
		return true;
	}

	@Override
	public void setType(ArgType type) {
		// allow set only initial type
		if (Objects.equals(this.type, type)) {
			super.setType(type);
		} else {
			throw new JadxRuntimeException("Can't change arg with immutable type");
		}
	}

	@Override
	public RegisterArg duplicate() {
		return duplicate(getRegNum(), getSVar());
	}

	@Override
	public RegisterArg duplicate(int regNum, SSAVar sVar) {
		RegisterArg dup = new TypeImmutableArg(regNum, getInitType());
		if (sVar != null) {
			dup.setSVar(sVar);
		}
		dup.copyAttributesFrom(this);
		return dup;
	}
}
