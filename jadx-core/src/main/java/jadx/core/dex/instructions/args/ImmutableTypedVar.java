package jadx.core.dex.instructions.args;

public class ImmutableTypedVar extends TypedVar {

	public ImmutableTypedVar(ArgType initType) {
		super(initType);
	}

	@Override
	public boolean forceSetType(ArgType newType) {
		return false;
	}

	@Override
	public boolean merge(TypedVar typedVar) {
		return false;
	}

	@Override
	public boolean merge(ArgType mtype) {
		return false;
	}
}
