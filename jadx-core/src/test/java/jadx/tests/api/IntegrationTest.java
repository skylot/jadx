package jadx.tests.api;

import jadx.api.DefaultJadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JadxInternalAccess;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;
import jadx.tests.api.compiler.DynamicCompiler;
import jadx.tests.api.compiler.StaticCompiler;
import jadx.tests.api.utils.TestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class IntegrationTest extends TestUtils {

	private static final String TEST_DIRECTORY = "src/test/java";
	private static final String TEST_DIRECTORY2 = "jadx-core/" + TEST_DIRECTORY;

	protected boolean outputCFG = false;
	protected boolean isFallback = false;
	protected boolean deleteTmpFiles = true;
	protected boolean withDebugInfo = true;

	protected Map<Integer, String> resMap = Collections.emptyMap();

	protected String outDir = "test-out-tmp";

	protected boolean compile = true;
	private DynamicCompiler dynamicCompiler;

	public ClassNode getClassNode(Class<?> clazz) {
		try {
			File jar = getJarForClass(clazz);
			return getClassNodeFromFile(jar, clazz.getName());
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return null;
	}

	public ClassNode getClassNodeFromFile(File file, String clsName) {
		JadxDecompiler d = new JadxDecompiler();
		try {
			d.loadFile(file);
		} catch (JadxException e) {
			fail(e.getMessage());
		}
		RootNode root = JadxInternalAccess.getRoot(d);
		root.getResourcesNames().putAll(resMap);

		ClassNode cls = root.searchClassByName(clsName);
		assertNotNull("Class not found: " + clsName, cls);
		assertEquals(cls.getFullName(), clsName);

		cls.load();
		for (IDexTreeVisitor visitor : getPasses()) {
			DepthTraversal.visit(visitor, cls);
		}
		// don't unload class

		System.out.println("-----------------------------------------------------------");
		System.out.println(cls.getCode());
		System.out.println("-----------------------------------------------------------");

		checkCode(cls);
		compile(cls);
		runAutoCheck(clsName);
		return cls;
	}

	private static void checkCode(ClassNode cls) {
		assertTrue("Inconsistent cls: " + cls,
				!cls.contains(AFlag.INCONSISTENT_CODE) && !cls.contains(AType.JADX_ERROR));
		for (MethodNode mthNode : cls.getMethods()) {
			assertTrue("Inconsistent method: " + mthNode,
					!mthNode.contains(AFlag.INCONSISTENT_CODE) && !mthNode.contains(AType.JADX_ERROR));
		}
		assertThat(cls.getCode().toString(), not(containsString("inconsistent")));
	}

	protected List<IDexTreeVisitor> getPasses() {
		return Jadx.getPassesList(new DefaultJadxArgs() {
			@Override
			public boolean isCFGOutput() {
				return outputCFG;
			}

			@Override
			public boolean isRawCFGOutput() {
				return outputCFG;
			}

			@Override
			public boolean isFallbackMode() {
				return isFallback;
			}

			@Override
			public boolean isShowInconsistentCode() {
				return true;
			}

			@Override
			public int getThreadsCount() {
				return 1;
			}

			@Override
			public boolean isSkipResources() {
				return true;
			}
		}, new File(outDir));
	}

	private void runAutoCheck(String clsName) {
		try {
			// run 'check' method from original class
			Class<?> origCls;
			try {
				origCls = Class.forName(clsName);
			} catch (ClassNotFoundException e) {
				// ignore
				return;
			}
			Method checkMth;
			try {
				checkMth = origCls.getMethod("check");
			} catch (NoSuchMethodException e) {
				// ignore
				return;
			}
			if (!checkMth.getReturnType().equals(void.class)
					|| !Modifier.isPublic(checkMth.getModifiers())
					|| Modifier.isStatic(checkMth.getModifiers())) {
				fail("Wrong 'check' method");
				return;
			}
			try {
				checkMth.invoke(origCls.newInstance());
			} catch (InvocationTargetException ie) {
				rethrow("Java check failed", ie);
			}
			// run 'check' method from decompiled class
			try {
				invoke("check");
			} catch (InvocationTargetException ie) {
				rethrow("Decompiled check failed", ie);
			}
			System.out.println("Auto check: PASSED");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Auto check exception: " + e.getMessage());
		}
	}

	private void rethrow(String msg, InvocationTargetException ie) {
		Throwable cause = ie.getCause();
		if (cause instanceof AssertionError) {
			System.err.println(msg);
			throw ((AssertionError) cause);
		} else {
			cause.printStackTrace();
			fail(msg + cause.getMessage());
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

	void compile(ClassNode cls) {
		if (!compile) {
			return;
		}
		try {
			dynamicCompiler = new DynamicCompiler(cls);
			boolean result = dynamicCompiler.compile();
			assertTrue("Compilation failed", result);
			System.out.println("Compilation: PASSED");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public Object invoke(String method) throws Exception {
		return invoke(method, new Class[0]);
	}

	public Object invoke(String method, Class[] types, Object... args) throws Exception {
		Method mth = getReflectMethod(method, types);
		return invoke(mth, args);
	}

	public Method getReflectMethod(String method, Class... types) {
		assertNotNull("dynamicCompiler not ready", dynamicCompiler);
		try {
			return dynamicCompiler.getMethod(method, types);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	public Object invoke(Method mth, Object... args) throws Exception {
		assertNotNull("dynamicCompiler not ready", dynamicCompiler);
		assertNotNull("unknown method", mth);
		return dynamicCompiler.invoke(mth, args);
	}

	public File getJarForClass(Class<?> cls) throws IOException {
		String path = cls.getPackage().getName().replace('.', '/');
		List<File> list;
		if (!withDebugInfo) {
			list = compileClass(cls);
		} else {
			list = getClassFilesWithInners(cls);
			if (list.isEmpty()) {
				list = compileClass(cls);
			}
		}
		assertNotEquals("File list is empty", 0, list.size());

		File temp = createTempFile(".jar");
		JarOutputStream jo = new JarOutputStream(new FileOutputStream(temp));
		for (File file : list) {
			FileUtils.addFileToJar(jo, file, path + "/" + file.getName());
		}
		jo.close();
		return temp;
	}

	protected File createTempFile(String suffix) {
		File temp = null;
		try {
			temp = File.createTempFile("jadx-tmp-", System.nanoTime() + suffix);
			if (deleteTmpFiles) {
				temp.deleteOnExit();
			} else {
				System.out.println("Temporary file path: " + temp.getAbsolutePath());
			}
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return temp;
	}

	private static File createTempDir(String prefix) throws IOException {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		String baseName = prefix + "-" + System.nanoTime();
		for (int counter = 1; counter < 1000; counter++) {
			File tempDir = new File(baseDir, baseName + counter);
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}
		throw new IOException("Failed to create temp directory");
	}

	private List<File> getClassFilesWithInners(Class<?> cls) {
		List<File> list = new ArrayList<File>();
		String pkgName = cls.getPackage().getName();
		URL pkgResource = ClassLoader.getSystemClassLoader().getResource(pkgName.replace('.', '/'));
		if (pkgResource != null) {
			try {
				String clsName = cls.getName();
				File directory = new File(pkgResource.toURI());
				String[] files = directory.list();
				for (String file : files) {
					String fullName = pkgName + "." + file;
					if (fullName.startsWith(clsName)) {
						list.add(new File(directory, file));
					}
				}
			} catch (URISyntaxException e) {
				fail(e.getMessage());
			}
		}
		return list;
	}

	private List<File> compileClass(Class<?> cls) throws IOException {
		String fileName = cls.getName();
		int end = fileName.indexOf('$');
		if (end != -1) {
			fileName = fileName.substring(0, end);
		}
		fileName = fileName.replace('.', '/') + ".java";
		File file = new File(TEST_DIRECTORY, fileName);
		if (!file.exists()) {
			file = new File(TEST_DIRECTORY2, fileName);
		}
		assertTrue("Test source file not found: " + fileName, file.exists());

		File outTmp = createTempDir("jadx-tmp-classes");
		outTmp.deleteOnExit();
		List<File> files = StaticCompiler.compile(Arrays.asList(file), outTmp, withDebugInfo);
		// remove classes which are parents for test class
		Iterator<File> iterator = files.iterator();
		while (iterator.hasNext()) {
			File next = iterator.next();
			if (!next.getName().contains(cls.getSimpleName())) {
				iterator.remove();
			}
		}
		for (File clsFile : files) {
			clsFile.deleteOnExit();
		}
		return files;
	}

	public void setResMap(Map<Integer, String> resMap) {
		this.resMap = resMap;
	}

	protected void noDebugInfo() {
		this.withDebugInfo = false;
	}

	protected void setFallback() {
		this.isFallback = true;
	}

	protected void disableCompilation() {
		this.compile = false;
	}

	// Use only for debug purpose
	@Deprecated
	protected void setOutputCFG() {
		this.outputCFG = true;
	}

	// Use only for debug purpose
	@Deprecated
	protected void notDeleteTmpJar() {
		this.deleteTmpFiles = false;
	}
}
