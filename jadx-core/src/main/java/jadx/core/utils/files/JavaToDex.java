package jadx.core.utils.files;

import jadx.core.utils.exceptions.JadxException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.android.dx.command.DxConsole;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

public class JavaToDex {

	public static class DxArgs extends Arguments {
		public DxArgs(String dexFile, String[] input) {
			outName = dexFile;
			fileNames = input;
			jarOutput = false;

			optimize = true;
			localInfo = true;
			coreLibrary = true;
		}
	}

	private String dxErrors;

	public byte[] convert(String javaFile) throws JadxException {

		ByteArrayOutputStream err_out = new ByteArrayOutputStream();
		DxConsole.err = new PrintStream(err_out);

		PrintStream old_out = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(baos));
			DxArgs args = new DxArgs("-", new String[] { javaFile });
			Main.run(args);
			baos.close();
		} catch (Throwable e) {
			throw new JadxException("dx exception: " + e.getMessage(), e);
		} finally {
			System.setOut(old_out);
		}

		// err_out also contains warnings
		dxErrors = err_out.toString();
		return baos.toByteArray();
	}

	public String getDxErrors() {
		return dxErrors;
	}

	public boolean isError() {
		return dxErrors != null && dxErrors.length() > 0;
	}
}
