package jadx.plugins.input.dex.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;

public class SmaliTestUtils {

	public static Path compileSmaliFromResource(String res) {
		try {
			Path input = Paths.get(ClassLoader.getSystemResource(res).toURI());
			return compileSmali(input);
		} catch (Exception e) {
			throw new AssertionError("Smali assemble error", e);
		}
	}

	public static Path compileSmali(Path input) {
		try {
			Path tempFile = Files.createTempFile("jadx", "smali.dex");
			compileSmali(tempFile, Collections.singletonList(input));
			return tempFile;
		} catch (Exception e) {
			throw new AssertionError("Smali assemble error", e);
		}
	}

	private static void compileSmali(Path output, List<Path> inputFiles) {
		try {
			SmaliOptions options = new SmaliOptions();
			options.outputDexFile = output.toAbsolutePath().toString();
			List<String> inputFileNames = inputFiles.stream()
					.map(Path::toAbsolutePath)
					.map(Path::toString)
					.collect(Collectors.toList());
			Smali.assemble(options, inputFileNames);
		} catch (Exception e) {
			throw new AssertionError("Smali assemble error", e);
		}
	}
}
