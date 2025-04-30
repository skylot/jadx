package jadx.core.export;

import java.io.File;

import jadx.core.utils.files.FileUtils;

public class OutDirs {
	private final File srcOutDir;
	private final File resOutDir;

	public OutDirs(File srcOutDir, File resOutDir) {
		this.srcOutDir = srcOutDir;
		this.resOutDir = resOutDir;
	}

	public File getSrcOutDir() {
		return srcOutDir;
	}

	public File getResOutDir() {
		return resOutDir;
	}

	public void makeDirs() {
		FileUtils.makeDirs(srcOutDir);
		FileUtils.makeDirs(resOutDir);
	}
}
