package jadx.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;

import static jadx.core.utils.files.FileUtils.close;

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
			close(in);
		}
		return className;
	}

}
