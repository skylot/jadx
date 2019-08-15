package jadx.core.dex.instructions;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.RegisterArg;

public interface CallMthInterface {

	MethodInfo getCallMth();

	@Nullable
	RegisterArg getInstanceArg();

	/**
	 * Return offset to match method args from {@link #getCallMth()}
	 */
	int getFirstArgOffset();
}
