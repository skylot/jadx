package jadx.core.export.gen;

import java.io.File;
import java.util.List;

import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.files.FileUtils;

public class GradleGeneratorTools {

	public static String guessProjectName(RootNode root) {
		List<File> inputFiles = root.getArgs().getInputFiles();
		if (inputFiles.size() == 1) {
			return FileUtils.getPathBaseName(inputFiles.get(0).toPath());
		}
		// default
		return "PROJECT_NAME";
	}
}
