package jadx.core.dex.visitors.typeinference;

import java.util.Objects;

import jadx.core.dex.instructions.args.ArgType;

public final class TypeBoundConst implements ITypeBound {
	private final BoundEnum bound;
	private final ArgType type;

	public TypeBoundConst(BoundEnum bound, ArgType type) {
		this.bound = bound;
		this.type = type;
	}

	@Override
	public BoundEnum getBound() {
		return bound;
	}

	@Override
	public ArgType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypeBoundConst that = (TypeBoundConst) o;
		return bound == that.bound &&
				Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bound, type);
	}

	@Override
	public String toString() {
		return "{" + bound + ": " + type + '}';
	}
}
