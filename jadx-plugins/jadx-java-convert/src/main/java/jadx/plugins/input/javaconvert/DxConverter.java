package jadx.plugins.input.javaconvert;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;

public class DxConverter {
	private static final String CHARSET_NAME = "UTF-8";

	private static class DxArgs extends com.android.dx.command.dexer.Main.Arguments {
		public DxArgs(DxContext context, String dexDir, String[] input) {
			super(context);
			outName = dexDir;
			fileNames = input;
			jarOutput = false;
			multiDex = true;

			optimize = true;
			localInfo = true;
			coreLibrary = true;

			debug = true;
			warnings = true;
			minSdkVersion = 28;
		}
	}

	public static void run(Path path, Path tempDirectory) {
		int result;
		String dxErrors;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ByteArrayOutputStream errOut = new ByteArrayOutputStream()) {
			DxContext context = new DxContext(out, errOut);
			DxArgs args = new DxArgs(
					context,
					tempDirectory.toAbsolutePath().toString(),
					new String[] { path.toAbsolutePath().toString() });
			result = new Main(context).runDx(args);
			dxErrors = errOut.toString(CHARSET_NAME);
		} catch (Exception e) {
			throw new RuntimeException("dx exception: " + e.getMessage(), e);
		}
		if (result != 0) {
			throw new RuntimeException("Java to dex conversion error, code: " + result + ", errors: " + dxErrors);
		}
	}
}
