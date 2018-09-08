package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

public final class NamedArg extends InsnArg implements Named {

	@NotNull
	private String name;

	public NamedArg(@NotNull String name, @NotNull ArgType type) {
		this.name = name;
		this.type = type;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@Override
	public boolean isNamed() {
		return true;
	}

	public void setName(@NotNull String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NamedArg)) {
			return false;
		}
		return name.equals(((NamedArg) o).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "(" + name + " " + type + ")";
	}
}
