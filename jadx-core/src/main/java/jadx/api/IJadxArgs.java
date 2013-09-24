package jadx.api;

public interface IJadxArgs {
	int getThreadsCount();

	boolean isCFGOutput();

	boolean isRawCFGOutput();

	boolean isFallbackMode();

	boolean isVerbose();
}
