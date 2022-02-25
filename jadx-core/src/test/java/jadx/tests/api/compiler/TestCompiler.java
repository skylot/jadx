package jadx.tests.api.compiler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.jetbrains.annotations.NotNull;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.files.FileUtils;
import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestCompiler implements Closeable {
	private final CompilerOptions options;
	private final JavaCompiler compiler;
	private final ClassFileManager fileManager;

	public TestCompiler(CompilerOptions options) {
		this.options = options;
		int javaVersion = options.getJavaVersion();
		if (!JavaUtils.checkJavaVersion(javaVersion)) {
			throw new IllegalArgumentException("Current java version not meet requirement: "
					+ "current: " + JavaUtils.JAVA_VERSION_INT + ", required: " + javaVersion);
		}
		if (options.isUseEclipseCompiler()) {
			compiler = new EclipseCompiler();
		} else {
			compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new IllegalStateException("Can not find compiler, please use JDK instead");
			}
		}
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	}

	public List<File> compileFiles(List<File> sourceFiles, Path outTmp) throws IOException {
		List<JavaFileObject> jfObjects = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
		boolean success = compile(jfObjects);
		if (!success) {
			return Collections.emptyList();
		}
		List<File> files = new ArrayList<>();
		for (JavaClassObject classObject : fileManager.getClassLoader().getClassObjects()) {
			Path path = outTmp.resolve(classObject.getName().replace('.', '/') + ".class");
			FileUtils.makeDirsForFile(path);
			Files.write(path, classObject.getBytes());
			files.add(path.toFile());
		}
		return files;
	}

	public boolean compileNodes(List<ClassNode> clsNodeList) {
		List<JavaFileObject> jfObjects = new ArrayList<>(clsNodeList.size());
		for (ClassNode clsNode : clsNodeList) {
			jfObjects.add(new StringJavaFileObject(clsNode.getFullName(), clsNode.getCode().getCodeStr()));
		}
		return compile(jfObjects);
	}

	private boolean compile(List<JavaFileObject> jfObjects) {
		List<String> arguments = new ArrayList<>();
		arguments.add(options.isIncludeDebugInfo() ? "-g" : "-g:none");
		int javaVersion = options.getJavaVersion();
		String javaVerStr = javaVersion <= 8 ? "1." + javaVersion : Integer.toString(javaVersion);
		arguments.add("-source");
		arguments.add(javaVerStr);
		arguments.add("-target");
		arguments.add(javaVerStr);
		arguments.addAll(options.getArguments());

		CompilationTask compilerTask = compiler.getTask(null, fileManager, null, arguments, null, jfObjects);
		return Boolean.TRUE.equals(compilerTask.call());
	}

	private ClassLoader getClassLoader() {
		return fileManager.getClassLoader();
	}

	public Class<?> getClass(String clsFullName) throws ClassNotFoundException {
		return getClassLoader().loadClass(clsFullName);
	}

	@NotNull
	public Method getMethod(Class<?> cls, String methodName, Class<?>[] types) throws NoSuchMethodException {
		return cls.getMethod(methodName, types);
	}

	public Object invoke(String clsFullName, String methodName, Class<?>[] types, Object[] args) {
		try {
			for (Class<?> type : types) {
				checkType(type);
			}
			Class<?> cls = getClass(clsFullName);
			Method mth = getMethod(cls, methodName, types);
			Object inst = cls.getConstructor().newInstance();
			assertNotNull(mth, "Failed to get method " + methodName + '(' + Arrays.toString(types) + ')');
			return mth.invoke(inst, args);
		} catch (Throwable e) {
			IntegrationTest.rethrow("Invoke error", e);
			return null;
		}
	}

	private Class<?> checkType(Class<?> type) throws ClassNotFoundException {
		if (type.isPrimitive()) {
			return type;
		}
		if (type.isArray()) {
			return checkType(type.getComponentType());
		}
		Class<?> cls = getClassLoader().loadClass(type.getName());
		if (type != cls) {
			throw new IllegalArgumentException("Internal test class cannot be used in method invoke");
		}
		return cls;
	}

	@Override
	public void close() throws IOException {
		fileManager.close();
	}
}
