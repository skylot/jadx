package jadx.api;

import java.io.File;

public interface IJadxArgs {
	File getOutDir();

	int getThreadsCount();

	boolean isCFGOutput();

	boolean isRawCFGOutput();

	boolean isFallbackMode();

	boolean isShowInconsistentCode();

	boolean isVerbose();

	boolean isSkipResources();

	boolean isSkipSources();

	boolean isDeobfuscationOn();

	int getDeobfuscationMinLength();

	int getDeobfuscationMaxLength();

	boolean isDeobfuscationForceSave();

	boolean useSourceNameAsClassAlias();
}
