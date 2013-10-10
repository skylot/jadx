package jadx.core.dex.instructions.args;

public class ImmutableTypedVar extends TypedVar {

	public ImmutableTypedVar(ArgType type) {
		super(type);
	}

	@Override
	public boolean isImmutable() {
		return true;
	}

	@Override
	public void forceSetType(ArgType newType) {
	}

	@Override
	public boolean merge(TypedVar typedVar) {
		return false;
	}

	@Override
	public boolean merge(ArgType type) {
		return false;
	}
}
