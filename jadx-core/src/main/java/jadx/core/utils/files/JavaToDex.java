package jadx.core.utils.files;

import java.io.ByteArrayOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

import jadx.core.utils.exceptions.JadxException;

public class JavaToDex {

	private static final String CHARSET_NAME = "UTF-8";

	private static class DxArgs extends Arguments {
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

	private String dxErrors;

	public List<Path> convert(Path jar) throws JadxException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ByteArrayOutputStream errOut = new ByteArrayOutputStream()) {
			DxContext context = new DxContext(out, errOut);
			Path dir = Files.createTempDirectory("jadx");
			DxArgs args = new DxArgs(
					context,
					dir.toAbsolutePath().toString(),
					new String[] { jar.toAbsolutePath().toString() });
			int result = new Main(context).runDx(args);
			dxErrors = errOut.toString(CHARSET_NAME);
			if (result != 0) {
				throw new JadxException("Java to dex conversion error, code: " + result);
			}
			List<Path> list = new ArrayList<>();
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
				for (Path child : ds) {
					list.add(child);
					child.toFile().deleteOnExit();
				}
			}
			dir.toFile().deleteOnExit();
			return list;
		} catch (Exception e) {
			throw new JadxException("dx exception: " + e.getMessage(), e);
		}
	}

	public String getDxErrors() {
		return dxErrors;
	}

	public boolean isError() {
		return dxErrors != null && !dxErrors.isEmpty();
	}
}
