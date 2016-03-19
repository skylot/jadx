package jadx.core.utils.files;

import jadx.core.utils.exceptions.JadxException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import com.android.dx.command.DxConsole;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

import static com.android.dx.command.dexer.Main.run;
import static jadx.core.utils.files.FileUtils.close;
import static java.lang.System.setOut;

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
		ByteArrayOutputStream errOut = new ByteArrayOutputStream();
		try {
			DxConsole.err = new PrintStream(errOut, true, CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new JadxException(e.getMessage(), e);
		}
		PrintStream oldOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			setOut(new PrintStream(baos, true, CHARSET_NAME));
			DxArgs args = new DxArgs("-", new String[]{javaFile});
			resetOutDexVar();
			run(args);
		} catch (Throwable e) {
			throw new JadxException("dx exception: " + e.getMessage(), e);
		} finally {
			close(baos);
			System.setOut(oldOut);
		}
		try {
			// errOut also contains warnings
			dxErrors = errOut.toString(CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new JadxException("Can't save error output", e);
		}
		return baos.toByteArray();
	}

	private void resetOutDexVar() throws JadxException {
		try {
			Field outputDex = Main.class.getDeclaredField("outputDex");
			outputDex.setAccessible(true);
			outputDex.set(null, null);
		} catch (Exception e) {
			throw new JadxException("Failed to reset outputDex field", e);
		}
	}

	public String getDxErrors() {
		return dxErrors;
	}

	public boolean isError() {
		return dxErrors != null && !dxErrors.isEmpty();
	}
}
