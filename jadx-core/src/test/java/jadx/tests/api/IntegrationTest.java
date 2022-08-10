package jadx.tests.api;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CommentsLevel;
import jadx.api.DecompilationMode;
import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.api.JavaClass;
import jadx.api.args.GeneratedRenamesMappingFileMode;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.DebugChecks;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.ResourceStorage;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.tests.api.compiler.CompilerOptions;
import jadx.tests.api.compiler.JavaUtils;
import jadx.tests.api.compiler.TestCompiler;
import jadx.tests.api.utils.TestUtils;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class IntegrationTest extends TestUtils {
	private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

	private static final String TEST_DIRECTORY = "src/test/java";
	private static final String TEST_DIRECTORY2 = "jadx-core/" + TEST_DIRECTORY;

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
	private CompilerOptions compilerOptions;

	private boolean saveTestJar = false;

	protected Map<Integer, String> resMap = Collections.emptyMap();

	private boolean allowWarnInCode;
	private boolean printLineNumbers;
	private boolean printOffsets;
	private boolean printDisassemble;
	private @Nullable Boolean useJavaInput;
	private boolean removeParentClassOnInput;

	private @Nullable TestCompiler sourceCompiler;
	private @Nullable TestCompiler decompiledCompiler;

	/**
	 * Run check method on decompiled code even if source check method not found.
	 * Useful for smali test if check method added to smali code
	 */
	private boolean forceDecompiledCheck = false;

	static {
		// enable debug checks
		DebugChecks.checksEnabled = true;
	}

	protected JadxDecompiler jadxDecompiler;

	@BeforeEach
	public void init() {
		this.compile = true;
		this.compilerOptions = new CompilerOptions();
		this.resMap = Collections.emptyMap();
		this.removeParentClassOnInput = true;
		this.useJavaInput = null;

		args = new JadxArgs();
		args.setOutDir(new File("test-out-tmp"));
		args.setShowInconsistentCode(true);
		args.setThreadsCount(1);
		args.setSkipResources(true);
		args.setFsCaseSensitive(false); // use same value on all systems
		args.setCommentsLevel(CommentsLevel.DEBUG);
		args.setDeobfuscationOn(false);
		args.setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode.IGNORE);
	}

	@AfterEach
	public void after() throws IOException {
		FileUtils.clearTempRootDir();
		close(jadxDecompiler);
		close(sourceCompiler);
		close(decompiledCompiler);
	}

	private void close(Closeable closeable) throws IOException {
		if (closeable != null) {
			closeable.close();
		}
	}

	public void setOutDirSuffix(String suffix) {
		args.setOutDir(new File("test-out-" + suffix + "-tmp"));
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
			return null;
		}
	}

	public List<ClassNode> getClassNodes(Class<?>... classes) {
		try {
			assertThat("Class list is empty", classes, not(emptyArray()));
			List<File> srcFiles = Stream.of(classes).map(this::getSourceFileForClass).collect(Collectors.toList());
			List<File> clsFiles = compileSourceFiles(srcFiles);
			assertThat("Class files list is empty", clsFiles, not(empty()));
			return decompileFiles(clsFiles);
		} catch (Exception e) {
			LOG.error("Failed to get class node", e);
			fail(e.getMessage());
			return null;
		}
	}

	public ClassNode getClassNodeFromFiles(List<File> files, String clsName) {
		jadxDecompiler = loadFiles(files);
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);

		ClassNode cls = root.resolveClass(clsName);
		assertThat("Class not found: " + clsName, cls, notNullValue());
		if (removeParentClassOnInput) {
			assertThat(clsName, is(cls.getClassInfo().getFullName()));
		} else {
			LOG.info("Convert back to top level: {}", cls);
			cls.getTopParentClass().decompile(); // keep correct process order
			cls.getClassInfo().notInner(root);
			cls.updateParentClass();
		}
		decompileAndCheck(cls);
		return cls;
	}

	public List<ClassNode> decompileFiles(List<File> files) {
		jadxDecompiler = loadFiles(files);
		List<ClassNode> sortedClsNodes = jadxDecompiler.getDecompileScheduler()
				.buildBatches(jadxDecompiler.getClasses())
				.stream()
				.flatMap(Collection::stream)
				.map(JavaClass::getClassNode)
				.collect(Collectors.toList());
		decompileAndCheck(sortedClsNodes);
		return sortedClsNodes;
	}

	@NotNull
	public ClassNode searchTestCls(List<ClassNode> list, String shortClsName) {
		return searchCls(list, getTestPkg() + '.' + shortClsName);
	}

	@NotNull
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
		clsList.forEach(cls -> checkCode(cls, allowWarnInCode));
		compileClassNode(clsList);
		clsList.forEach(this::runAutoCheck);
	}

	private void printDisasm(ClassNode cls) {
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println(cls.getDisassembledCode());
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}

	private void printCodeWithLineNumbers(ICodeInfo code) {
		String codeStr = code.getCodeStr();
		Map<Integer, Integer> lineMapping = code.getCodeMetadata().getLineMapping();
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
		ICodeMetadata metadata = code.getCodeMetadata();
		int lineStartPos = 0;
		int newLineLen = ICodeWriter.NL.length();
		for (String line : codeStr.split(ICodeWriter.NL)) {
			Object ann = metadata.getAt(lineStartPos);
			String offsetStr = "";
			if (ann instanceof InsnCodeOffset) {
				int offset = ((InsnCodeOffset) ann).getOffset();
				offsetStr = "/* " + leftPad(String.valueOf(offset), 5) + " */";
			}
			System.out.println(rightPad(offsetStr, 12) + line);
			lineStartPos += line.length() + newLineLen;
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

	private void runAutoCheck(ClassNode cls) {
		String clsName = cls.getClassInfo().getRawName().replace('/', '.');
		try {
			// run 'check' method from original class
			boolean sourceCheckFound = runSourceAutoCheck(clsName);

			// run 'check' method from decompiled class
			if (compile && (sourceCheckFound || forceDecompiledCheck)) {
				runDecompiledAutoCheck(cls);
			}
		} catch (Exception e) {
			LOG.error("Auto check failed", e);
			fail("Auto check exception: " + e.getMessage());
		}
	}

	private boolean runSourceAutoCheck(String clsName) {
		if (sourceCompiler == null) {
			System.out.println("Source check: no code");
			return false;
		}
		Class<?> origCls;
		try {
			origCls = sourceCompiler.getClass(clsName);
		} catch (ClassNotFoundException e) {
			rethrow("Missing class: " + clsName, e);
			return false;
		}
		Method checkMth;
		try {
			checkMth = sourceCompiler.getMethod(origCls, CHECK_METHOD_NAME, new Class[] {});
		} catch (NoSuchMethodException e) {
			// ignore
			return false;
		}
		if (!checkMth.getReturnType().equals(void.class)
				|| !Modifier.isPublic(checkMth.getModifiers())
				|| Modifier.isStatic(checkMth.getModifiers())) {
			fail("Wrong 'check' method");
			return false;
		}
		try {
			limitExecTime(() -> checkMth.invoke(origCls.getConstructor().newInstance()));
			System.out.println("Source check: PASSED");
			return true;
		} catch (Throwable e) {
			throw new JadxRuntimeException("Source check failed", e);
		}
	}

	public void runDecompiledAutoCheck(ClassNode cls) {
		try {
			limitExecTime(() -> invoke(decompiledCompiler, cls.getFullName(), CHECK_METHOD_NAME));
			System.out.println("Decompiled check: PASSED");
		} catch (Throwable e) {
			rethrow("Decompiled check failed", e);
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
			rethrow(msg, e.getCause());
		} else if (e instanceof AssertionError) {
			System.err.println(msg);
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

	void compileClassNode(List<ClassNode> clsList) {
		if (!compile) {
			return;
		}
		try {
			// TODO: eclipse uses files or compilation units providers added in Java 9
			compilerOptions.setUseEclipseCompiler(false);
			decompiledCompiler = new TestCompiler(compilerOptions);
			decompiledCompiler.compileNodes(clsList);
			System.out.println("Compilation: PASSED");
		} catch (Exception e) {
			fail(e);
		}
	}

	public Object invoke(TestCompiler compiler, String clsFullName, String method) throws Exception {
		assertNotNull(compiler, "compiler not ready");
		return compiler.invoke(clsFullName, method, new Class<?>[] {}, new Object[] {});
	}

	private List<File> compileClass(Class<?> cls) throws IOException {
		File sourceFile = getSourceFileForClass(cls);
		List<File> clsFiles = compileSourceFiles(Collections.singletonList(sourceFile));
		if (removeParentClassOnInput) {
			// remove classes which are parents for test class
			String clsFullName = cls.getName();
			String clsName = clsFullName.substring(clsFullName.lastIndexOf('.') + 1);
			clsFiles.removeIf(next -> !next.getName().contains(clsName));
		}
		return clsFiles;
	}

	private File getSourceFileForClass(Class<?> cls) {
		String clsFullName = cls.getName();
		int innerEnd = clsFullName.indexOf('$');
		String rootClsName = innerEnd == -1 ? clsFullName : clsFullName.substring(0, innerEnd);
		String javaFileName = rootClsName.replace('.', '/') + ".java";
		File file = new File(TEST_DIRECTORY, javaFileName);
		if (file.exists()) {
			return file;
		}
		File file2 = new File(TEST_DIRECTORY2, javaFileName);
		if (file2.exists()) {
			return file2;
		}
		throw new JadxRuntimeException("Test source not found for class: " + clsFullName);
	}

	private List<File> compileSourceFiles(List<File> compileFileList) throws IOException {
		Path outTmp = FileUtils.createTempDir("jadx-tmp-classes");
		sourceCompiler = new TestCompiler(compilerOptions);
		List<File> files = sourceCompiler.compileFiles(compileFileList, outTmp);
		if (saveTestJar) {
			saveToJar(files, outTmp);
		}
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

	public CompilerOptions getCompilerOptions() {
		return compilerOptions;
	}

	public void setArgs(JadxArgs args) {
		this.args = args;
	}

	public void setResMap(Map<Integer, String> resMap) {
		this.resMap = resMap;
	}

	protected void noDebugInfo() {
		this.compilerOptions.setIncludeDebugInfo(false);
	}

	public void useEclipseCompiler() {
		Assumptions.assumeTrue(JavaUtils.checkJavaVersion(11), "eclipse compiler library using Java 11");
		this.compilerOptions.setUseEclipseCompiler(true);
	}

	public void useTargetJavaVersion(int version) {
		Assumptions.assumeTrue(JavaUtils.checkJavaVersion(version), "skip test for higher java version");
		this.compilerOptions.setJavaVersion(version);
	}

	protected void setFallback() {
		disableCompilation();
		this.args.setDecompilationMode(DecompilationMode.FALLBACK);
	}

	protected void disableCompilation() {
		this.compile = false;
	}

	protected void forceDecompiledCheck() {
		this.forceDecompiledCheck = true;
	}

	protected void enableDeobfuscation() {
		args.setDeobfuscationOn(true);
		args.setGeneratedRenamesMappingFileMode(GeneratedRenamesMappingFileMode.IGNORE);
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

	public void useDexInput(String mode) {
		useDexInput();
		this.getArgs().getPluginOptions().put("java-convert.mode", mode);
	}

	protected boolean isJavaInput() {
		return Utils.getOrElse(useJavaInput, USE_JAVA_INPUT);
	}

	public void keepParentClassOnInput() {
		this.removeParentClassOnInput = false;
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
