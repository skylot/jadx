package jadx.core.utils.files;

import java.io.ByteArrayOutputStream;

import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

import jadx.core.utils.exceptions.JadxException;

public class JavaToDex {

	private static final String CHARSET_NAME = "UTF-8";

	private static class DxArgs extends Arguments {
		public DxArgs(DxContext context, String dexFile, String[] input) {
			super(context);
			outName = dexFile;
			fileNames = input;
			jarOutput = false;

			optimize = true;
			localInfo = true;
			coreLibrary = true;

			debug = true;
			warnings = true;
			minSdkVersion = 28;
		}
	}

	private String dxErrors;

	public byte[] convert(String javaFile) throws JadxException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
		     ByteArrayOutputStream errOut = new ByteArrayOutputStream()) {
			DxContext context = new DxContext(out, errOut);
			DxArgs args = new DxArgs(context, "-", new String[]{javaFile});
			int result = (new Main(context)).runDx(args);
			dxErrors = errOut.toString(CHARSET_NAME);
			if (result != 0) {
				throw new JadxException("Java to dex conversion error, code: " + result);
			}
			return out.toByteArray();
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
