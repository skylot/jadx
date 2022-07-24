package jadx.tests.api;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import jadx.api.JadxInternalAccess;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public abstract class SmaliTest extends IntegrationTest {

	private static final String SMALI_TESTS_DIR = "src/test/smali";
	private static final String SMALI_TESTS_EXT = ".smali";

	private String currentProject = "jadx-core";

	public void setCurrentProject(String currentProject) {
		this.currentProject = currentProject;
	}

	@BeforeEach
	public void init() {
		Assumptions.assumeFalse(USE_JAVA_INPUT, "skip smali test for java input tests");
		super.init();
		this.useDexInput();
	}

	protected ClassNode getClassNodeFromSmali(String file, String clsName) {
		File smaliFile = getSmaliFile(file);
		return getClassNodeFromFiles(Collections.singletonList(smaliFile), clsName);
	}

	/**
	 * Preferred method for one file smali test
	 */
	protected ClassNode getClassNodeFromSmali() {
		return getClassNodeFromSmaliWithPkg(getTestPkg(), getTestName());
	}

	protected ClassNode getClassNodeFromSmaliWithClsName(String fullClsName) {
		return getClassNodeFromSmali(getTestPkg() + File.separatorChar + getTestName(), fullClsName);
	}

	protected ClassNode getClassNodeFromSmaliWithPath(String path, String clsName) {
		return getClassNodeFromSmali(path + File.separatorChar + clsName, clsName);
	}

	protected ClassNode getClassNodeFromSmaliWithPkg(String pkg, String clsName) {
		return getClassNodeFromSmali(pkg + File.separatorChar + clsName, pkg + '.' + clsName);
	}

	protected ClassNode getClassNodeFromSmaliFiles(String pkg, String testName, String clsName) {
		return getClassNodeFromFiles(collectSmaliFiles(pkg, testName), pkg + '.' + clsName);
	}

	protected ClassNode getClassNodeFromSmaliFiles(String clsName) {
		return searchCls(loadFromSmaliFiles(), getTestPkg() + '.' + clsName);
	}

	protected ClassNode getClassNodeFromSmaliFiles() {
		return searchCls(loadFromSmaliFiles(), getTestPkg() + '.' + getTestName());
	}

	protected List<ClassNode> loadFromSmaliFiles() {
		jadxDecompiler = loadFiles(collectSmaliFiles(getTestPkg(), getTestName()));
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);
		List<ClassNode> classes = root.getClasses(false);
		decompileAndCheck(classes);
		return classes;
	}

	private List<File> collectSmaliFiles(String pkg, @Nullable String testDir) {
		String smaliFilesDir;
		if (testDir == null) {
			smaliFilesDir = pkg + File.separatorChar;
		} else {
			smaliFilesDir = pkg + File.separatorChar + testDir + File.separatorChar;
		}
		File smaliDir = getSmaliDir(smaliFilesDir);
		String[] smaliFileNames = smaliDir.list((dir, name) -> name.endsWith(".smali"));
		assertThat("Smali files not found in " + smaliDir, smaliFileNames, notNullValue());
		return Stream.of(smaliFileNames)
				.map(file -> new File(smaliDir, file))
				.collect(Collectors.toList());
	}

	private File getSmaliFile(String baseName) {
		File smaliFile = new File(SMALI_TESTS_DIR, baseName + SMALI_TESTS_EXT);
		if (smaliFile.exists()) {
			return smaliFile;
		}
		File pathFromRoot = new File(currentProject, smaliFile.getPath());
		if (pathFromRoot.exists()) {
			return pathFromRoot;
		}
		throw new AssertionError("Smali file not found: " + smaliFile.getPath());
	}

	private File getSmaliDir(String baseName) {
		File smaliDir = new File(SMALI_TESTS_DIR, baseName);
		if (smaliDir.exists()) {
			return smaliDir;
		}
		File pathFromRoot = new File(currentProject, smaliDir.getPath());
		if (pathFromRoot.exists()) {
			return pathFromRoot;
		}
		throw new AssertionError("Smali dir not found: " + smaliDir.getPath());
	}
}
