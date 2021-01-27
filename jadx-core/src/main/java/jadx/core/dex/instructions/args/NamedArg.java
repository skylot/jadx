package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.NotNull;

public final class NamedArg extends InsnArg implements Named, VisibleVar {

	@NotNull
	private String name;

	private int index = -1;

	public NamedArg(@NotNull String name, @NotNull ArgType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@Override
	public boolean isNamed() {
		return true;
	}

	@Override
	public void setName(@NotNull String name) {
		this.name = name;
	}

	@Override
	public InsnArg duplicate() {
		return copyCommonParams(new NamedArg(name, type));
	}

	@Override
	public int hashCode() {
		return name.hashCode();
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
	public String toString() {
		return '(' + name + ' ' + type + ')';
	}
}
