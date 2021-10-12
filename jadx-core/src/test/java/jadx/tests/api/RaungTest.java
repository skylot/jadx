package jadx.tests.api;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;

import jadx.api.JadxInternalAccess;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public abstract class RaungTest extends IntegrationTest {

	private static final String RAUNG_TESTS_PROJECT = "jadx-core";
	private static final String RAUNG_TESTS_DIR = "src/test/raung";
	private static final String RAUNG_TESTS_EXT = ".raung";

	@BeforeEach
	public void init() {
		super.init();
		this.useJavaInput();
	}

	/**
	 * Preferred method for one file raung test
	 */
	protected ClassNode getClassNodeFromRaung() {
		String pkg = getTestPkg();
		String clsName = getTestName();
		return getClassNodeFromRaung(pkg + File.separatorChar + clsName, pkg + '.' + clsName);
	}

	protected ClassNode getClassNodeFromRaung(String file, String clsName) {
		File raungFile = getRaungFile(file);
		return getClassNodeFromFiles(Collections.singletonList(raungFile), clsName);
	}

	protected List<ClassNode> loadFromRaungFiles() {
		jadxDecompiler = loadFiles(collectRaungFiles(getTestPkg(), getTestName()));
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);
		List<ClassNode> classes = root.getClasses(false);
		decompileAndCheck(classes);
		return classes;
	}

	private List<File> collectRaungFiles(String pkg, String testDir) {
		String raungFilesDir = pkg + File.separatorChar + testDir + File.separatorChar;
		File raungDir = getRaungDir(raungFilesDir);
		String[] raungFileNames = raungDir.list((dir, name) -> name.endsWith(".raung"));
		assertThat("Raung files not found in " + raungDir, raungFileNames, notNullValue());
		return Stream.of(raungFileNames)
				.map(file -> new File(raungDir, file))
				.collect(Collectors.toList());
	}

	private static File getRaungFile(String baseName) {
		File raungFile = new File(RAUNG_TESTS_DIR, baseName + RAUNG_TESTS_EXT);
		if (raungFile.exists()) {
			return raungFile;
		}
		File pathFromRoot = new File(RAUNG_TESTS_PROJECT, raungFile.getPath());
		if (pathFromRoot.exists()) {
			return pathFromRoot;
		}
		throw new AssertionError("Raung file not found: " + raungFile.getPath());
	}

	private static File getRaungDir(String baseName) {
		File raungDir = new File(RAUNG_TESTS_DIR, baseName);
		if (raungDir.exists()) {
			return raungDir;
		}
		File pathFromRoot = new File(RAUNG_TESTS_PROJECT, raungDir.getPath());
		if (pathFromRoot.exists()) {
			return pathFromRoot;
		}
		throw new AssertionError("Raung dir not found: " + raungDir.getPath());
	}
}
