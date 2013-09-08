package jadx.api;

public class DefaultJadxArgs implements IJadxArgs {

	@Override
	public int getThreadsCount() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public boolean isCFGOutput() {
		return false;
	}

	@Override
	public boolean isRawCFGOutput() {
		return false;
	}

	@Override
	public boolean isFallbackMode() {
		return false;
	}

	@Override
	public boolean isVerbose() {
		return false;
	}
}
