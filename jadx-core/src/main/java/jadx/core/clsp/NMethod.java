package jadx.core.clsp;

import jadx.core.dex.instructions.args.ArgType;

/**
 * Generic method node in classpath graph.
 */
public class NMethod {

	private final String shortId;
	private final ArgType[] argType;
	private final ArgType retType;
	private final boolean varArgs;

	public NMethod(String shortId, ArgType[] argType, ArgType retType, boolean varArgs) {
		this.shortId = shortId;
		this.argType = argType;
		this.retType = retType;
		this.varArgs = varArgs;
	}

	public String getShortId() {
		return shortId;
	}

	public ArgType[] getArgType() {
		return argType;
	}

	public ArgType getReturnType() {
		return retType;
	}

	public boolean isVarArgs() {
		return varArgs;
	}
}
