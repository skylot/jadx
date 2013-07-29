package jadx.core.dex.instructions.args;

public final class NamedArg extends InsnArg {

	private String name;

	public NamedArg(String name, ArgType type) {
		this.name = name;
		this.typedVar = new TypedVar(type);
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean isNamed() {
		return true;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "(" + name + " " + typedVar + ")";
	}
}
