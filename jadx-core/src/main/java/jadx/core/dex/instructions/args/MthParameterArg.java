package jadx.core.dex.instructions.args;

public class MthParameterArg extends RegisterArg {

	private boolean isThis = false;

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

	public void markAsThis() {
		this.isThis = true;
	}

	@Override
	public boolean isThis() {
		return isThis;
	}

	@Override
	public String getName() {
		if (isThis) {
			return "this";
		}
		return super.getName();
	}

	@Override
	void setSVar(SSAVar sVar) {
		if (isThis) {
			sVar.setName("this");
		}
		super.setSVar(sVar);
	}
}
