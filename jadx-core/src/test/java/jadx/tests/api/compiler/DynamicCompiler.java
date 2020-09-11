package jadx.tests.api.compiler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static javax.tools.JavaCompiler.CompilationTask;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DynamicCompiler {

	private static final Logger LOG = LoggerFactory.getLogger(DynamicCompiler.class);

	private final List<ClassNode> clsNodeList;
	private JavaFileManager fileManager;

	public DynamicCompiler(List<ClassNode> clsNodeList) {
		this.clsNodeList = clsNodeList;
	}

	public boolean compile() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			LOG.error("Can not find compiler, please use JDK instead");
			return false;
		}
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));

		List<JavaFileObject> jFiles = new ArrayList<>(clsNodeList.size());
		for (ClassNode clsNode : clsNodeList) {
			jFiles.add(new CharSequenceJavaFileObject(clsNode.getFullName(), clsNode.getCode().toString()));
		}

		CompilationTask compilerTask = compiler.getTask(null, fileManager, null, null, null, jFiles);
		return Boolean.TRUE.equals(compilerTask.call());
	}

	private ClassLoader getClassLoader() {
		return fileManager.getClassLoader(null);
	}

	public Object makeInstance(ClassNode cls) throws Exception {
		String fullName = cls.getFullName();
		return getClassLoader().loadClass(fullName).getConstructor().newInstance();
	}

	@NotNull
	public Method getMethod(Object inst, String methodName, Class<?>[] types) throws Exception {
		for (Class<?> type : types) {
			checkType(type);
		}
		return inst.getClass().getMethod(methodName, types);
	}

	public Object invoke(ClassNode cls, String methodName, Class<?>[] types, Object[] args) {
		try {
			Object inst = makeInstance(cls);
			Method reflMth = getMethod(inst, methodName, types);
			assertNotNull(reflMth, "Failed to get method " + methodName + '(' + Arrays.toString(types) + ')');
			return reflMth.invoke(inst, args);
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
		Class<?> decompiledCls = getClassLoader().loadClass(type.getName());
		if (type != decompiledCls) {
			throw new IllegalArgumentException("Internal test class cannot be used in method invoke");
		}
		return decompiledCls;
	}
}
