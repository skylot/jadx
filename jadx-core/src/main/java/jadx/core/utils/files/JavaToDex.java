package jadx.core.utils.files;

import java.io.ByteArrayOutputStream;

import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

import jadx.core.utils.exceptions.JadxException;

import static jadx.core.utils.files.FileUtils.close;

public class JavaToDex {

	private static final String CHARSET_NAME = "UTF-8";

	public static class DxArgs extends Arguments {
		public DxArgs(String dexFile, String[] input) {
			outName = dexFile;
			fileNames = input;
			jarOutput = false;

			optimize = true;
			localInfo = true;
			coreLibrary = true;

			debug = true;
		}
	}

	private String dxErrors;

	public byte[] convert(String javaFile) throws JadxException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream errOut = new ByteArrayOutputStream();
		try {
			DxContext context = new DxContext(out, errOut);
			DxArgs args = new DxArgs("-", new String[]{javaFile});
			int result = (new Main(context)).runDx(args);
			if (result != 0) {
				throw new JadxException("Java to dex conversion error, code: " + result);
			}
			dxErrors = errOut.toString(CHARSET_NAME);
			return out.toByteArray();
		} catch (Exception e) {
			throw new JadxException("dx exception: " + e.getMessage(), e);
		} finally {
			close(out);
			close(errOut);
		}
	}

	public String getDxErrors() {
		return dxErrors;
	}

	public boolean isError() {
		return dxErrors != null && !dxErrors.isEmpty();
	}
}
