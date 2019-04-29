package jadx.tests.api.compiler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;

import static javax.tools.JavaCompiler.CompilationTask;

public class DynamicCompiler {

	private static final Logger LOG = LoggerFactory.getLogger(DynamicCompiler.class);

	private final ClassNode clsNode;

	private JavaFileManager fileManager;

	private Object instance;

	public DynamicCompiler(ClassNode clsNode) {
		this.clsNode = clsNode;
	}

	public boolean compile() throws Exception {
		String fullName = clsNode.getFullName();
		String code = clsNode.getCode().toString();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			LOG.error("Can not find compiler, please use JDK instead");
			return false;
		}
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));

		List<JavaFileObject> jFiles = new ArrayList<>(1);
		jFiles.add(new CharSequenceJavaFileObject(fullName, code));

		CompilationTask compilerTask = compiler.getTask(null, fileManager, null, null, null, jFiles);
		return Boolean.TRUE.equals(compilerTask.call());
	}

	private ClassLoader getClassLoader() {
		return fileManager.getClassLoader(null);
	}

	private void makeInstance() throws Exception {
		String fullName = clsNode.getFullName();
		instance = getClassLoader().loadClass(fullName).getConstructor().newInstance();
	}

	private Object getInstance() throws Exception {
		if (instance == null) {
			makeInstance();
		}
		return instance;
	}

	public Method getMethod(String method, Class<?>[] types) throws Exception {
		for (Class<?> type : types) {
			checkType(type);
		}
		return getInstance().getClass().getMethod(method, types);
	}

	public Object invoke(Method mth, Object... args) throws Exception {
		return mth.invoke(getInstance(), args);
	}

	private Class<?> checkType(Class<?> type) throws ClassNotFoundException {
		if (type.isPrimitive()) {
			return type;
		}
		if (type.isArray()) {
			return checkType(type.getComponentType());
		}
		Class<?> decompiledCls = getClassLoader().loadClass(type.getName());
		if (type != decompiledCls) {
			throw new IllegalArgumentException("Internal test class cannot be used in method invoke");
		}
		return decompiledCls;
	}
}
