package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;

public final class TypeUpdateEntry implements Comparable<TypeUpdateEntry> {
	private final int seq;
	private final InsnArg arg;
	private final ArgType type;

	public TypeUpdateEntry(int seq, InsnArg arg, ArgType type) {
		this.seq = seq;
		this.arg = arg;
		this.type = type;
	}

	public int getSeq() {
		return seq;
	}

	public InsnArg getArg() {
		return arg;
	}

	public ArgType getType() {
		return type;
	}

	@Override
	public int compareTo(@NotNull TypeUpdateEntry other) {
		return Integer.compare(this.seq, other.seq);
	}

	@Override
	public String toString() {
		return type + " -> " + arg.toShortString() + " in " + arg.getParentInsn();
	}
}
