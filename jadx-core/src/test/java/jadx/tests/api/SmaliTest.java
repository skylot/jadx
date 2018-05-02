package jadx.tests.api;

import java.io.File;

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
		compileSmali(smaliFile, outDex);
		return getClassNodeFromFile(outDex, clsName);
	}

	protected ClassNode getClassNodeFromSmaliWithPath(String path, String clsName) {
		return getClassNodeFromSmali(path + File.separatorChar + clsName, clsName);
	}

	protected ClassNode getClassNodeFromSmali(String clsName) {
		return getClassNodeFromSmali(clsName, clsName);
	}

	private static File getSmaliFile(String clsName) {
		File smaliFile = new File(SMALI_TESTS_DIR, clsName + SMALI_TESTS_EXT);
		if (smaliFile.exists()) {
			return smaliFile;
		}
		smaliFile = new File(SMALI_TESTS_PROJECT, smaliFile.getPath());
		if (smaliFile.exists()) {
			return smaliFile;
		}
		throw new AssertionError("Smali file not found: " + smaliFile.getAbsolutePath());
	}

	private static boolean compileSmali(File input, File output) {
		try {
			SmaliOptions params = new SmaliOptions();
			params.outputDexFile = output.getAbsolutePath();
			Smali.assemble(params, input.getAbsolutePath());
		} catch (Exception e) {
			throw new AssertionError("Smali assemble error", e);
		}
		return true;
	}
}
