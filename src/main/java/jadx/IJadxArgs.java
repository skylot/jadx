package jadx;

import jadx.utils.files.InputFile;

import java.io.File;
import java.util.List;

public interface IJadxArgs {
    File getOutDir();

    int getThreadsCount();

    boolean isCFGOutput();

    boolean isRawCFGOutput();

    List<InputFile> getInput();

    boolean isFallbackMode();

    boolean isVerbose();

    boolean isPrintHelp();
}
