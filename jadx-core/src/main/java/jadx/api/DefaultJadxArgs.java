package jadx.api;

import java.io.File;

public class DefaultJadxArgs implements IJadxArgs {

	@Override
	public File getOutDir() {
		return new File("jadx-output");
	}

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
	public boolean isShowInconsistentCode() {
		return false;
	}

	@Override
	public boolean isVerbose() {
		return false;
	}

	@Override
	public boolean isSkipResources() {
		return false;
	}

	@Override
	public boolean isSkipSources() {
		return false;
	}

	@Override
	public boolean isDeobfuscationOn() {
		return false;
	}

	@Override
	public int getDeobfuscationMinLength() {
		return Integer.MIN_VALUE + 1;
	}

	@Override
	public int getDeobfuscationMaxLength() {
		return Integer.MAX_VALUE - 1;
	}

	@Override
	public boolean isDeobfuscationForceSave() {
		return false;
	}
}
