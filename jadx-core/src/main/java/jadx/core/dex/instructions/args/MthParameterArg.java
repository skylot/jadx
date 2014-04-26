package jadx.core.dex.instructions.args;

public class MthParameterArg extends RegisterArg {

	public MthParameterArg(int rn, ArgType type) {
		super(rn, type);
	}

	@Override
	public boolean isTypeImmutable() {
		return true;
	}

	@Override
	public void setType(ArgType type) {
	}
}
