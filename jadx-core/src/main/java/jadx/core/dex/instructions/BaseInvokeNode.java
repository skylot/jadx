package jadx.core.dex.instructions;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;

public abstract class BaseInvokeNode extends InsnNode {
	public BaseInvokeNode(InsnType type, int argsCount) {
		super(type, argsCount);
	}

	public abstract MethodInfo getCallMth();

	@Nullable
	public abstract InsnArg getInstanceArg();

	public abstract boolean isStaticCall();

	/**
	 * Return offset to match method args from {@link #getCallMth()}
	 */
	public abstract int getFirstArgOffset();
}
