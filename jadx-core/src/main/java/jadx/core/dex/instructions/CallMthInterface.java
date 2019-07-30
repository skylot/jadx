package jadx.core.dex.instructions;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.RegisterArg;

public interface CallMthInterface {

	MethodInfo getCallMth();

	RegisterArg getInstanceArg();

	/**
	 * Return offset to match method args from {@link #getCallMth()}
	 */
	int getFirstArgOffset();
}
