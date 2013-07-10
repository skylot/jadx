package jadx.api;

import java.io.File;
import java.util.List;

public interface IJadxArgs {
	List<File> getInput();

    File getOutDir();

    int getThreadsCount();

    boolean isCFGOutput();

    boolean isRawCFGOutput();

    boolean isFallbackMode();

    boolean isVerbose();

    boolean isPrintHelp();
}
