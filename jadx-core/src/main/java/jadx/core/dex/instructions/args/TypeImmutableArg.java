package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

public class TypeImmutableArg extends RegisterArg {

	private boolean isThis;

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
	void setSVar(@NotNull SSAVar sVar) {
		if (isThis) {
			sVar.setName("this");
		}
		sVar.setTypeImmutable(type);
		super.setSVar(sVar);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeImmutableArg)) {
			return false;
		}
		if (!super.equals(obj)) {
			return false;
		}
		return isThis == ((TypeImmutableArg) obj).isThis;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + (isThis ? 1 : 0);
	}
}
