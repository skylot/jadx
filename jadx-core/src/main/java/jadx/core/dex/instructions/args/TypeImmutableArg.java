package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

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
		// not allowed
	}

	@Override
	void setSVar(@NotNull SSAVar sVar) {
		sVar.setTypeImmutable(type);
		super.setSVar(sVar);
	}
}
