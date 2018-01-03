package jadx.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;

public class AsmUtils {

	private AsmUtils() {
	}

	public static String getNameFromClassFile(File file) throws IOException {
		String className;
		try (FileInputStream in = new FileInputStream(file)) {
			ClassReader classReader = new ClassReader(in);
			className = classReader.getClassName();
		}
		return className;
	}
}
