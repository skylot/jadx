package jadx.tests.api;

import jadx.core.dex.nodes.ClassNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jf.smali.main;

import static org.junit.Assert.fail;

public class SmaliTest extends IntegrationTest {

	private static final String SMALI_TESTS_PROJECT = "jadx-core";
	private static final String SMALI_TESTS_DIR = "src/test/smali";
	private static final String SMALI_TESTS_EXT = ".smali";

	protected ClassNode getClassNodeFromSmali(String clsName) {
		File smaliFile = getSmaliFile(clsName);
		File outDex = createTempFile(".dex");
		compileSmali(smaliFile, outDex);
		return getClassNodeFromFile(outDex, clsName);
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
		fail("Smali file not found: " + SMALI_TESTS_DIR + "/" + clsName + SMALI_TESTS_EXT);
		return null;
	}

	private static boolean compileSmali(File input, File output) {
		List<String> args = new ArrayList<String>();
		args.add(input.getAbsolutePath());

		args.add("-o");
		args.add(output.getAbsolutePath());

		main.main(args.toArray(new String[args.size()]));
		return true;
	}
}
