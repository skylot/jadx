package jadx.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;

public class AsmUtils {

	private AsmUtils() {
	}

	public static String getNameFromClassFile(File file) throws IOException {
		String className = null;
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			ClassReader classReader = new ClassReader(in);
			className = classReader.getClassName();
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return className;
	}

}
