package jadx.tests.api;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;

import jadx.core.dex.nodes.ClassNode;

public abstract class SmaliTest extends IntegrationTest {

	private static final String SMALI_TESTS_PROJECT = "jadx-core";
	private static final String SMALI_TESTS_DIR = "src/test/smali";
	private static final String SMALI_TESTS_EXT = ".smali";

	protected ClassNode getClassNodeFromSmali(String file, String clsName) {
		File smaliFile = getSmaliFile(file);
		File outDex = createTempFile(".dex");
		compileSmali(outDex, Collections.singletonList(smaliFile));
		return getClassNodeFromFile(outDex, clsName);
	}

	protected ClassNode getClassNodeFromSmaliWithPath(String path, String clsName) {
		return getClassNodeFromSmali(path + File.separatorChar + clsName, clsName);
	}

	protected ClassNode getClassNodeFromSmaliWithPkg(String pkg, String clsName) {
		return getClassNodeFromSmali(pkg + File.separatorChar + clsName, pkg + '.' + clsName);
	}

	protected ClassNode getClassNodeFromSmaliFiles(String pkg, String testName, String clsName, String... smaliFileNames) {
		File outDex = createTempFile(".dex");
		List<File> smaliFiles = Arrays.stream(smaliFileNames)
				.map(file -> getSmaliFile(pkg + File.separatorChar + testName + File.separatorChar + file))
				.collect(Collectors.toList());
		compileSmali(outDex, smaliFiles);
		return getClassNodeFromFile(outDex, pkg + "." + clsName);
	}

	protected ClassNode getClassNodeFromSmali(String clsName) {
		return getClassNodeFromSmali(clsName, clsName);
	}

	private static File getSmaliFile(String baseName) {
		File smaliFile = new File(SMALI_TESTS_DIR, baseName + SMALI_TESTS_EXT);
		if (smaliFile.exists()) {
			return smaliFile;
		}
		File pathFromRoot = new File(SMALI_TESTS_PROJECT, smaliFile.getPath());
		if (pathFromRoot.exists()) {
			return pathFromRoot;
		}
		throw new AssertionError("Smali file not found: " + smaliFile.getPath());
	}

	private static boolean compileSmali(File output, List<File> inputFiles) {
		try {
			SmaliOptions params = new SmaliOptions();
			params.outputDexFile = output.getAbsolutePath();
			List<String> inputFileNames = inputFiles.stream().map(File::getAbsolutePath).collect(Collectors.toList());
			Smali.assemble(params, inputFileNames);
		} catch (Exception e) {
			throw new AssertionError("Smali assemble error", e);
		}
		return true;
	}
}
