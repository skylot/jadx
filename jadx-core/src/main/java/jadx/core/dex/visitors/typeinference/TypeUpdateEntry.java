package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;

public final class TypeUpdateEntry {
	private final InsnArg arg;
	private final ArgType type;

	public TypeUpdateEntry(InsnArg arg, ArgType type) {
		this.arg = arg;
		this.type = type;
	}

	public InsnArg getArg() {
		return arg;
	}

	public ArgType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "TypeUpdateEntry{" + arg + " -> " + type + '}';
	}
}
