package jadx.plugins.input.javaconvert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.objectweb.asm.ClassReader;

public class AsmUtils {

	public static String getNameFromClassFile(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			return getClassFullName(new ClassReader(in));
		}
	}

	public static String getNameFromClassFile(byte[] content) throws IOException {
		return getClassFullName(new ClassReader(content));
	}

	private static String getClassFullName(ClassReader classReader) {
		return classReader.getClassName();
	}

}
