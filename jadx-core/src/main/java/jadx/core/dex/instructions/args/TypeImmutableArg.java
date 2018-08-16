package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

public class TypeImmutableArg extends RegisterArg {

	public static final String THIS_ARG_NAME = "this";

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
	public String getName() {
		if (isThis()) {
			return THIS_ARG_NAME;
		}
		return super.getName();
	}

	@Override
	void setSVar(@NotNull SSAVar sVar) {
		if (isThis()) {
			sVar.setName(THIS_ARG_NAME);
		}
		sVar.setTypeImmutable(type);
		super.setSVar(sVar);
	}
}
