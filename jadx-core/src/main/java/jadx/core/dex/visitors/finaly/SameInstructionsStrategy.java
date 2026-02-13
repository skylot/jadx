package jadx.core.dex.visitors.finaly;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;

public abstract class SameInstructionsStrategy {

	public abstract boolean sameInsns(InsnNode dupInsn, InsnNode fInsn);

	public abstract boolean isSameArgs(InsnArg dupArg, InsnArg fArg);
}
