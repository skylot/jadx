package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;

public interface ITypeBound {
	BoundEnum getBound();

	ArgType getType();
}
