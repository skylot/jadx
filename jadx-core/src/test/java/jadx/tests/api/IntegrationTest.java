package jadx.tests.api;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.CommentsLevel;
import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.api.data.annotations.InsnCodeOffset;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.DebugChecks;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.ResourceStorage;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.tests.api.compiler.DynamicCompiler;
import jadx.tests.api.compiler.JavaUtils;
import jadx.tests.api.compiler.StaticCompiler;
import jadx.tests.api.utils.TestUtils;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class IntegrationTest extends TestUtils {
	private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

	private static final String TEST_DIRECTORY = "src/test/java";
	private static final String TEST_DIRECTORY2 = "jadx-core/" + TEST_DIRECTORY;

	private static final String OUT_DIR = "test-out-tmp";

	private static final String DEFAULT_INPUT_PLUGIN = "dx";
	/**
	 * Set 'TEST_INPUT_PLUGIN' env variable to use 'java' or 'dx' input in tests
	 */
	static final boolean USE_JAVA_INPUT = Utils.getOrElse(System.getenv("TEST_INPUT_PLUGIN"), DEFAULT_INPUT_PLUGIN).equals("java");

	/**
	 * Run auto check method if defined:
	 *
	 * <pre>
	 * public void check() {
	 * }
	 * </pre>
	 */
	private static final String CHECK_METHOD_NAME = "check";

	protected JadxArgs args;

	protected boolean compile;
	protected boolean withDebugInfo;
	protected boolean useEclipseCompiler;
	private int targetJavaVersion = 8;

	private boolean saveTestJar = false;

	protected Map<Integer, String> resMap = Collections.emptyMap();

	private boolean allowWarnInCode;
	private boolean printLineNumbers;
	private boolean printOffsets;
	private boolean printDisassemble;
	private Boolean useJavaInput = null;

	private DynamicCompiler dynamicCompiler;

	static {
		// enable debug checks
		DebugChecks.checksEnabled = true;
	}

	protected JadxDecompiler jadxDecompiler;

	@BeforeEach
	public void init() {
		this.withDebugInfo = true;
		this.compile = true;
		this.useEclipseCompiler = false;
		this.resMap = Collections.emptyMap();

		args = new JadxArgs();
		args.setOutDir(new File(OUT_DIR));
		args.setShowInconsistentCode(true);
		args.setThreadsCount(1);
		args.setSkipResources(true);
		args.setFsCaseSensitive(false); // use same value on all systems
		args.setCommentsLevel(CommentsLevel.DEBUG);
	}

	@AfterEach
	public void after() {
		FileUtils.clearTempRootDir();
		if (jadxDecompiler != null) {
			jadxDecompiler.close();
		}
	}

	public String getTestName() {
		return this.getClass().getSimpleName();
	}

	public String getTestPkg() {
		return this.getClass().getPackage().getName().replace("jadx.tests.integration.", "");
	}

	public ClassNode getClassNode(Class<?> clazz) {
		try {
			List<File> files = compileClass(clazz);
			assertThat("File list is empty", files, not(empty()));
			return getClassNodeFromFiles(files, clazz.getName());
		} catch (Exception e) {
			LOG.error("Failed to get class node", e);
			fail(e.getMessage());
		}
		return null;
	}

	public ClassNode getClassNodeFromFiles(List<File> files, String clsName) {
		jadxDecompiler = loadFiles(files);
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);

		ClassNode cls = root.resolveClass(clsName);
		assertThat("Class not found: " + clsName, cls, notNullValue());
		assertThat(clsName, is(cls.getClassInfo().getFullName()));

		decompileAndCheck(cls);
		return cls;
	}

	@Nullable
	public ClassNode searchCls(List<ClassNode> list, String clsName) {
		for (ClassNode cls : list) {
			if (cls.getClassInfo().getFullName().equals(clsName)) {
				return cls;
			}
		}
		for (ClassNode cls : list) {
			if (cls.getClassInfo().getShortName().equals(clsName)) {
				return cls;
			}
		}
		fail("Class not found by name " + clsName + " in list: " + list);
		return null;
	}

	protected JadxDecompiler loadFiles(List<File> inputFiles) {
		args.setInputFiles(inputFiles);
		boolean useDx = !isJavaInput();
		LOG.info(useDx ? "Using dex input" : "Using java input");
		args.setUseDxInput(useDx);

		JadxDecompiler d = new JadxDecompiler(args);
		try {
			d.load();
		} catch (Exception e) {
			LOG.error("Load failed", e);
			d.close();
			fail(e.getMessage());
			return null;
		}
		RootNode root = JadxInternalAccess.getRoot(d);
		insertResources(root);
		return d;
	}

	protected void decompileAndCheck(ClassNode cls) {
		decompileAndCheck(Collections.singletonList(cls));
	}

	protected void decompileAndCheck(List<ClassNode> clsList) {
		clsList.forEach(cls -> cls.add(AFlag.DONT_UNLOAD_CLASS)); // keep error and warning attributes
		clsList.forEach(ClassNode::decompile);

		for (ClassNode cls : clsList) {
			System.out.println("-----------------------------------------------------------");
			ICodeInfo code = cls.getCode();
			if (printLineNumbers) {
				printCodeWithLineNumbers(code);
			} else if (printOffsets) {
				printCodeWithOffsets(code);
			} else {
				System.out.println(code);
			}
		}
		System.out.println("-----------------------------------------------------------");
		if (printDisassemble) {
			clsList.forEach(this::printDisasm);
		}
		runChecks(clsList);
	}

	public void runChecks(ClassNode cls) {
		runChecks(Collections.singletonList(cls));
	}

	protected void runChecks(List<ClassNode> clsList) {
		clsList.forEach(this::checkCode);
		compile(clsList);
		clsList.forEach(this::runAutoCheck);
	}

	private void printDisasm(ClassNode cls) {
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println(cls.getDisassembledCode());
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}

	private void printCodeWithLineNumbers(ICodeInfo code) {
		String codeStr = code.getCodeStr();
		Map<Integer, Integer> lineMapping = code.getLineMapping();
		String[] lines = codeStr.split(ICodeWriter.NL);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int curLine = i + 1;
			String lineNumStr = "/* " + leftPad(String.valueOf(curLine), 3) + " */";
			Integer sourceLine = lineMapping.get(curLine);
			if (sourceLine != null) {
				lineNumStr += " /* " + sourceLine + " */";
			}
			System.out.println(rightPad(lineNumStr, 20) + line);
		}
	}

	private void printCodeWithOffsets(ICodeInfo code) {
		String codeStr = code.getCodeStr();
		Map<CodePosition, Object> annotations = code.getAnnotations();
		String[] lines = codeStr.split(ICodeWriter.NL);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int curLine = i + 1;
			Object ann = annotations.get(new CodePosition(curLine, 0));
			String offsetStr = "";
			if (ann instanceof InsnCodeOffset) {
				int offset = ((InsnCodeOffset) ann).getOffset();
				offsetStr = "/* " + leftPad(String.valueOf(offset), 5) + " */";
			}
			System.out.println(rightPad(offsetStr, 12) + line);
		}
	}

	private void insertResources(RootNode root) {
		if (resMap.isEmpty()) {
			return;
		}
		ResourceStorage resStorage = new ResourceStorage();
		for (Map.Entry<Integer, String> entry : resMap.entrySet()) {
			Integer id = entry.getKey();
			String name = entry.getValue();
			String[] parts = name.split("\\.");
			resStorage.add(new ResourceEntry(id, "", parts[0], parts[1], ""));
		}
		root.processResources(resStorage);
	}

	protected void checkCode(ClassNode cls) {
		assertFalse(hasErrors(cls), "Inconsistent cls: " + cls);
		for (MethodNode mthNode : cls.getMethods()) {
			if (hasErrors(mthNode)) {
				fail("Method with problems: " + mthNode
						+ "\n " + Utils.listToString(mthNode.getAttributesStringsList(), "\n "));
			}
		}

		String code = cls.getCode().getCodeStr();
		assertThat(code, not(containsString("inconsistent")));
		assertThat(code, not(containsString("JADX ERROR")));
	}

	private boolean hasErrors(IAttributeNode node) {
		if (node.contains(AFlag.INCONSISTENT_CODE) || node.contains(AType.JADX_ERROR)) {
			return true;
		}
		if (!allowWarnInCode) {
			JadxCommentsAttr commentsAttr = node.get(AType.JADX_COMMENTS);
			if (commentsAttr != null) {
				return commentsAttr.getComments().get(CommentsLevel.WARN) != null;
			}
		}
		return false;
	}

	private void runAutoCheck(ClassNode cls) {
		String clsName = cls.getClassInfo().getFullName();
		try {
			// run 'check' method from original class
			if (runSourceAutoCheck(clsName)) {
				return;
			}
			// run 'check' method from decompiled class
			if (compile) {
				runDecompiledAutoCheck(cls);
			}
		} catch (Exception e) {
			LOG.error("Auto check failed", e);
			fail("Auto check exception: " + e.getMessage());
		}
	}

	private boolean runSourceAutoCheck(String clsName) {
		Class<?> origCls;
		try {
			origCls = Class.forName(clsName);
		} catch (ClassNotFoundException e) {
			// ignore
			return true;
		}
		Method checkMth;
		try {
			checkMth = origCls.getMethod(CHECK_METHOD_NAME);
		} catch (NoSuchMethodException e) {
			// ignore
			return true;
		}
		if (!checkMth.getReturnType().equals(void.class)
				|| !Modifier.isPublic(checkMth.getModifiers())
				|| Modifier.isStatic(checkMth.getModifiers())) {
			fail("Wrong 'check' method");
			return true;
		}
		try {
			limitExecTime(() -> checkMth.invoke(origCls.getConstructor().newInstance()));
			System.out.println("Source check: PASSED");
		} catch (Throwable e) {
			throw new JadxRuntimeException("Source check failed", e);
		}
		return false;
	}

	public void runDecompiledAutoCheck(ClassNode cls) {
		try {
			limitExecTime(() -> invoke(cls, "check"));
			System.out.println("Decompiled check: PASSED");
		} catch (Throwable e) {
			throw new JadxRuntimeException("Decompiled check failed", e);
		}
	}

	private <T> T limitExecTime(Callable<T> call) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<T> future = executor.submit(call);
		try {
			return future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			future.cancel(true);
			rethrow("Execution timeout", ex);
		} catch (Throwable ex) {
			rethrow(ex.getMessage(), ex);
		} finally {
			executor.shutdownNow();
		}
		return null;
	}

	public static void rethrow(String msg, Throwable e) {
		if (e instanceof InvocationTargetException) {
			rethrow(msg, e.getCause());
		} else if (e instanceof ExecutionException) {
			rethrow(e.getMessage(), e.getCause());
		} else if (e instanceof AssertionError) {
			throw (AssertionError) e;
		} else {
			throw new RuntimeException(msg, e);
		}
	}

	protected MethodNode getMethod(ClassNode cls, String method) {
		for (MethodNode mth : cls.getMethods()) {
			if (mth.getName().equals(method)) {
				return mth;
			}
		}
		fail("Method not found " + method + " in class " + cls);
		return null;
	}

	void compile(List<ClassNode> clsList) {
		if (!compile) {
			return;
		}
		try {
			dynamicCompiler = new DynamicCompiler(clsList);
			boolean result = dynamicCompiler.compile();
			assertTrue(result, "Compilation failed");
			System.out.println("Compilation: PASSED");
		} catch (Exception e) {
			fail(e);
		}
	}

	public Object invoke(ClassNode cls, String method) throws Exception {
		return invoke(cls, method, new Class<?>[0]);
	}

	public Object invoke(ClassNode cls, String methodName, Class<?>[] types, Object... args) throws Exception {
		assertNotNull(dynamicCompiler, "dynamicCompiler not ready");
		return dynamicCompiler.invoke(cls, methodName, types, args);
	}

	private List<File> compileClass(Class<?> cls) throws IOException {
		String clsFullName = cls.getName();
		String rootClsName;
		int end = clsFullName.indexOf('$');
		if (end != -1) {
			rootClsName = clsFullName.substring(0, end);
		} else {
			rootClsName = clsFullName;
		}
		String javaFileName = rootClsName.replace('.', '/') + ".java";
		File file = new File(TEST_DIRECTORY, javaFileName);
		if (!file.exists()) {
			file = new File(TEST_DIRECTORY2, javaFileName);
		}
		assertThat("Test source file not found: " + javaFileName, file.exists(), is(true));
		List<File> compileFileList = Collections.singletonList(file);

		Path outTmp = FileUtils.createTempDir("jadx-tmp-classes");
		List<File> files = StaticCompiler.compile(compileFileList, outTmp.toFile(), withDebugInfo, useEclipseCompiler, targetJavaVersion);
		files.forEach(File::deleteOnExit);
		if (saveTestJar) {
			saveToJar(files, outTmp);
		}
		// remove classes which are parents for test class
		String clsName = clsFullName.substring(clsFullName.lastIndexOf('.') + 1);
		files.removeIf(next -> !next.getName().contains(clsName));
		return files;
	}

	private void saveToJar(List<File> files, Path baseDir) throws IOException {
		Path jarFile = Files.createTempFile("tests-" + getTestName() + '-', ".jar");
		try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile))) {
			for (File file : files) {
				Path fullPath = file.toPath();
				Path relativePath = baseDir.relativize(fullPath);
				JarEntry entry = new JarEntry(relativePath.toString());
				jar.putNextEntry(entry);
				jar.write(Files.readAllBytes(fullPath));
				jar.closeEntry();
			}
		}
		LOG.info("Test jar saved to: {}", jarFile.toAbsolutePath());
	}

	public JadxArgs getArgs() {
		return args;
	}

	public void setArgs(JadxArgs args) {
		this.args = args;
	}

	public void setResMap(Map<Integer, String> resMap) {
		this.resMap = resMap;
	}

	protected void noDebugInfo() {
		this.withDebugInfo = false;
	}

	protected void useEclipseCompiler() {
		this.useEclipseCompiler = true;
	}

	public void useTargetJavaVersion(int version) {
		Assumptions.assumeTrue(JavaUtils.checkJavaVersion(version), "skip test for higher java version");
		this.targetJavaVersion = version;
	}

	protected void setFallback() {
		disableCompilation();
		this.args.setFallbackMode(true);
	}

	protected void disableCompilation() {
		this.compile = false;
	}

	protected void enableDeobfuscation() {
		args.setDeobfuscationOn(true);
		args.setDeobfuscationForceSave(true);
		args.setDeobfuscationMinLength(2);
		args.setDeobfuscationMaxLength(64);
	}

	protected void allowWarnInCode() {
		allowWarnInCode = true;
	}

	protected void printLineNumbers() {
		printLineNumbers = true;
	}

	protected void printOffsets() {
		printOffsets = true;
	}

	public void useJavaInput() {
		this.useJavaInput = true;
	}

	public void useDexInput() {
		Assumptions.assumeFalse(USE_JAVA_INPUT, "skip dex input tests");
		this.useJavaInput = false;
	}

	protected boolean isJavaInput() {
		return Utils.getOrElse(useJavaInput, USE_JAVA_INPUT);
	}

	// Use only for debug purpose
	protected void printDisassemble() {
		this.printDisassemble = true;
	}

	// Use only for debug purpose
	protected void saveTestJar() {
		this.saveTestJar = true;
	}
}
