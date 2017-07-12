package jadx.core.utils.files;

import com.android.jack.Jack;
import com.android.jack.Options;
import com.android.jack.api.v04.impl.Api04Feature;
import com.android.sched.util.config.cli.TokenIterator;
import com.android.sched.util.location.NoLocation;
import jadx.core.utils.exceptions.JadxException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.android.dx.command.DxConsole;
import com.android.dx.command.dexer.Main;
import com.android.dx.command.dexer.Main.Arguments;

import static com.android.dx.command.dexer.Main.run;
import static com.android.jack.Main.parseCommandLine;
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
		try {
			if (isMagic52(javaFile)) {
				resetOutDexVar();
				String tmp = System.getProperty("java.io.tmpdir");
				String[] args = new String[]{"--import", javaFile, "--output-dex", tmp};
				TokenIterator iterator = new TokenIterator(new NoLocation(), args);
				ArrayList list = new ArrayList();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Options options = parseCommandLine(list);
				Jack.checkAndRun(Api04Feature.class, options);
				FileInputStream fis = new FileInputStream(tmp + "classes.dex");
				int len = fis.available();
				if (len <= 0) {
					throw new JadxException("Can't convert jar file by jack");
				}
				byte[] dex = new byte[len];
				int read = fis.read(dex);
				if (read <= 0) {
					throw new JadxException("Can't convert jar file by jack");
				}
				close(fis);
				return dex;
			}
		} catch (Exception e) {
			throw new JadxException("Can't convert magic 52 jar file ", e);
		}
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

	private static boolean isMagic52(String file) {
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			Map<String, Integer> processedEntryNamesMap = new HashMap<String, Integer>();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				final String entryName = zipEntry.getName();
				if (!processedEntryNamesMap.containsKey(entryName)) {
					if (!zipEntry.isDirectory()) {
						if (entryName.endsWith(".class")) {
							int magic = getMagic(zip.getInputStream(zipEntry));
							return magic >= 52;
						} else {
							continue;
						}
					}
					processedEntryNamesMap.put(entryName, 1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(zip);
		}
		return false;
	}

	private static int getMagic(InputStream in) {
		byte[] b = new byte[8];
		try {
			int ret = in.read(b);
			if (ret < 0) {
				return 0;
			}
			return (int) b[7];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
