package jadx.core.dex.instructions.args;

public abstract class Typed {

	protected ArgType type;

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public boolean isTypeImmutable() {
		return false;
	}

	public boolean merge(ArgType newType) {
		ArgType m = ArgType.merge(type, newType);
		if (m != null && !m.equals(type)) {
			setType(m);
			return true;
		}
		return false;
	}

	public boolean merge(InsnArg arg) {
		return merge(arg.getType());
	}
}
