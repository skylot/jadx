package jadx.core.clsp;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;

/**
 * Generic method node in classpath graph.
 */
public class NMethod implements Comparable<NMethod> {

	private final String shortId;

	/**
	 * Array contains only generic args, others set to 'null', size can be less than total args count
	 */
	@Nullable
	private final ArgType[] genericArgs;

	@Nullable
	private final ArgType retType;

	private final boolean varArgs;

	public NMethod(String shortId, @Nullable ArgType[] genericArgs, @Nullable ArgType retType, boolean varArgs) {
		this.shortId = shortId;
		this.genericArgs = genericArgs;
		this.retType = retType;
		this.varArgs = varArgs;
	}

	public String getShortId() {
		return shortId;
	}

	@Nullable
	public ArgType[] getGenericArgs() {
		return genericArgs;
	}

	@Nullable
	public ArgType getGenericArg(int i) {
		ArgType[] args = this.genericArgs;
		if (args != null && i < args.length) {
			return args[i];
		}
		return null;
	}

	@Nullable
	public ArgType getReturnType() {
		return retType;
	}

	public boolean isVarArgs() {
		return varArgs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NMethod)) {
			return false;
		}
		NMethod other = (NMethod) o;
		return shortId.equals(other.shortId);
	}

	@Override
	public int hashCode() {
		return shortId.hashCode();
	}

	@Override
	public int compareTo(@NotNull NMethod other) {
		return this.shortId.compareTo(other.shortId);
	}

	@Override
	public String toString() {
		return "NMethod{'" + shortId + '\''
				+ ", argTypes=" + Arrays.toString(genericArgs)
				+ ", retType=" + retType
				+ ", varArgs=" + varArgs
				+ '}';
	}
}
